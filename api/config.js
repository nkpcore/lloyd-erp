/**
 * Lloyd ERP — Fleet Governance Configuration API (Vercel Serverless / Cloud)
 * Uses api/lib/db.js for instant non-hanging persistence with Redis and memory/disk fallbacks.
 */

const db = require('./lib/db');
const { verifyAdminAuth } = require('./lib/auth');

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-admin-key, Accept');
    res.setHeader('Cache-Control', 'no-store');
}

let cachedGhRelease = null;
let lastGhCheckTime = 0;

async function getLiveGitHubRelease() {
    const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
    const repo = process.env.GITHUB_REPOSITORY || 'nkpcore/lloyd-erp';
    const now = Date.now();

    if (cachedGhRelease && (now - lastGhCheckTime < 60000)) {
        return cachedGhRelease;
    }

    try {
        const headers = {
            'User-Agent': 'LloydERP-Config-Server',
            'Accept': 'application/vnd.github.v3+json'
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const resp = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, {
            headers,
            signal: AbortSignal.timeout(1500)
        });
        if (!resp.ok) return null;

        const release = await resp.json();
        const apkAsset = (release.assets || []).find(a => (a.name || '').toLowerCase().endsWith('.apk'));

        cachedGhRelease = {
            version: (release.tag_name || release.name || '').trim().replace(/^[vV]/, ''),
            download_url: apkAsset ? apkAsset.browser_download_url : null,
            notes: release.body || release.name || null
        };
        lastGhCheckTime = now;
        return cachedGhRelease;
    } catch (_) {
        return null;
    }
}

module.exports = async function handler(req, res) {
    setCors(res);

    if (req.method === 'OPTIONS') {
        res.status(204).end();
        return;
    }

    try {
        if (req.method === 'GET') {
            const config = await db.getConfig();

            // Prioritize live GitHub Release dynamically (strictly no hardcoded fallback)
            const gh = await getLiveGitHubRelease();
            if (gh && gh.version) {
                config.latest_version_name = gh.version;
                if (gh.download_url) {
                    config.download_url = gh.download_url;
                }
            } else if (!config.latest_version_name) {
                const telemetry = await db.getTelemetryRecords();
                if (telemetry.length > 0 && telemetry[0].app_version) {
                    config.latest_version_name = telemetry[0].app_version;
                }
            }

            // SECURITY: Strip ban lists from public response.
            // Devices must call POST /api/ban to check their own status — never enumerate all bans.
            const { banned_devices, banned_students, ...publicConfig } = config;
            void banned_devices; void banned_students; // explicitly excluded
            res.status(200).json(publicConfig);
            return;
        }

        if (req.method === 'POST') {
            let body = req.body;
            if (typeof body === 'string') {
                try { body = JSON.parse(body); } catch (_) {}
            }
            body = body || {};

            if (!verifyAdminAuth(req, body)) {
                res.status(401).json({ error: 'Unauthorized: Invalid admin password' });
                return;
            }

            const current = await db.getConfig();

            // Phase 1.2: Strict field allowlist — unknown/extra body properties are silently dropped
            const ALLOWED_CONFIG_KEYS = [
                'latest_version_code', 'latest_version_name', 'min_version_code',
                'download_url', 'broadcast_notice', 'mandatory_update', 'sha256',
                'maintenance_mode', 'maintenance_message', 'pause_updates'
            ];

            const update = {};
            for (const key of ALLOWED_CONFIG_KEYS) {
                if (body[key] === undefined) continue;
                update[key] = body[key];
            }

            // Type-safety gates
            if (update.latest_version_code !== undefined) {
                const vc = parseInt(update.latest_version_code);
                if (!Number.isInteger(vc) || vc <= 0) {
                    res.status(400).json({ error: 'latest_version_code must be a positive integer' });
                    return;
                }
                update.latest_version_code = vc;
            }
            if (update.min_version_code !== undefined) {
                const mvc = parseInt(update.min_version_code);
                if (!Number.isInteger(mvc) || mvc < 0) {
                    res.status(400).json({ error: 'min_version_code must be a non-negative integer' });
                    return;
                }
                update.min_version_code = mvc;
            }
            if (update.download_url !== undefined) {
                if (typeof update.download_url !== 'string' || !update.download_url.startsWith('https://')) {
                    res.status(400).json({ error: 'download_url must be an HTTPS URL' });
                    return;
                }
            }
            if (update.maintenance_mode !== undefined) {
                update.maintenance_mode = Boolean(update.maintenance_mode);
            }
            if (update.mandatory_update !== undefined) {
                update.mandatory_update = Boolean(update.mandatory_update);
            }
            if (update.pause_updates !== undefined) {
                update.pause_updates = Boolean(update.pause_updates);
            }

            // Merge: only persist allowed/type-checked fields on top of current
            const updated = { ...current, ...update };

            // Preserve ban arrays from db — body cannot mutate them via this endpoint (use /api/ban)
            updated.banned_devices = current.banned_devices;
            updated.banned_students = current.banned_students;

            await db.saveConfig(updated);

            // Strip bans from POST response as well
            const { banned_devices: _bd, banned_students: _bs, ...safeUpdated } = updated;
            res.status(200).json({ success: true, config: safeUpdated });
            return;
        }

        res.status(405).json({ error: 'Method not allowed' });
    } catch (err) {
        console.error('[API/CONFIG] Handler error:', err);
        const fallback = await db.getConfig();
        res.status(200).json(fallback);
    }
};
