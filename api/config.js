/**
 * Lloyd ERP — Fleet Governance Configuration API (Vercel Serverless / Cloud)
 * Uses api/lib/db.js for instant non-hanging persistence with Redis and memory/disk fallbacks.
 */

const db = require('./lib/db');

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept');
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
            download_url: apkAsset ? `/api/ota?download=latest` : null,
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
                if (!config.download_url || config.download_url.includes('downloads/LloydAttendance-latest.apk')) {
                    config.download_url = gh.download_url || config.download_url;
                }
            } else if (!config.latest_version_name) {
                const telemetry = await db.getTelemetryRecords();
                if (telemetry.length > 0 && telemetry[0].app_version) {
                    config.latest_version_name = telemetry[0].app_version;
                }
            }

            res.status(200).json(config);
            return;
        }

        if (req.method === 'POST') {
            let body = req.body;
            if (typeof body === 'string') {
                try { body = JSON.parse(body); } catch (_) {}
            }
            body = body || {};

            const current = await db.getConfig();
            const updated = {
                ...current,
                ...body,
                min_version_code: body.min_version_code !== undefined ? parseInt(body.min_version_code) : current.min_version_code,
                latest_version_name: body.latest_version_name !== undefined ? (body.latest_version_name || null) : current.latest_version_name,
                download_url: body.download_url !== undefined ? body.download_url : current.download_url,
                banned_devices: Array.isArray(body.banned_devices) ? [...new Set(body.banned_devices)] : current.banned_devices,
                banned_students: Array.isArray(body.banned_students) ? [...new Set(body.banned_students)] : current.banned_students,
                maintenance_mode: typeof body.maintenance_mode === 'boolean' ? body.maintenance_mode : current.maintenance_mode
            };

            await db.saveConfig(updated);
            res.status(200).json({ success: true, config: updated });
            return;
        }

        res.status(405).json({ error: 'Method not allowed' });
    } catch (err) {
        console.error('[API/CONFIG] Handler error:', err);
        const fallback = await db.getConfig();
        res.status(200).json(fallback);
    }
};
