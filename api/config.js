/**
 * Lloyd ERP — Fleet Governance Configuration API (Vercel Serverless / Cloud)
 * Supports Vercel KV / Upstash Redis with zero external npm dependencies.
 * ZERO HARDCODED VERSIONS: Computes latest version dynamically from live fleet telemetry.
 */

const fs = require('fs');
const path = require('path');

const DEFAULT_CONFIG = {
    min_version_code: 1,
    latest_version_name: null,
    download_url: '',
    banned_students: [],
    banned_devices: [],
    broadcast_notice: null,
    maintenance_mode: false,
    maintenance_message: 'Lloyd ERP service is currently undergoing routine maintenance.'
};

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept');
}

async function callKv(command, ...args) {
    const url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
    const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
    if (!url || !token) return null;

    try {
        const resp = await fetch(url.replace(/\/+$/, ''), {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify([command, ...args])
        });
        if (!resp.ok) return null;
        const data = await resp.json();
        return data.result;
    } catch (e) {
        console.warn('[KV] Error executing KV command:', e.message);
        return null;
    }
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

        const resp = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, { headers });
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
    } catch (e) {
        return null;
    }
}

async function getLiveFleetVersion() {
    try {
        // Check KV telemetry records
        const kvValues = await callKv('HVALS', 'lloyd_fleet_telemetry');
        if (Array.isArray(kvValues) && kvValues.length > 0) {
            const records = kvValues.map(v => {
                try { return typeof v === 'string' ? JSON.parse(v) : v; } catch (e) { return null; }
            }).filter(Boolean);

            const sorted = records
                .map(r => ({ name: (r.app_version || '').trim(), code: parseInt(r.version_code) || 0 }))
                .filter(v => v.name || v.code > 0)
                .sort((a, b) => b.code - a.code);

            if (sorted.length > 0) return sorted[0].name || null;
        }

        // Check local disk fallback
        const localPath = path.join(process.cwd(), 'fleet_telemetry.json');
        if (fs.existsSync(localPath)) {
            const raw = fs.readFileSync(localPath, 'utf8');
            const data = JSON.parse(raw);
            const list = Array.isArray(data) ? data : Object.values(data);
            const sorted = list
                .map(r => ({ name: (r.app_version || '').trim(), code: parseInt(r.version_code) || 0 }))
                .filter(v => v.name || v.code > 0)
                .sort((a, b) => b.code - a.code);

            if (sorted.length > 0) return sorted[0].name || null;
        }
    } catch (e) {}

    return null;
}

async function getConfig() {
    let cfg = null;
    // 1. Try Vercel KV / Upstash Redis
    const kvData = await callKv('GET', 'lloyd_fleet_config');
    if (kvData) {
        try {
            cfg = typeof kvData === 'string' ? JSON.parse(kvData) : kvData;
        } catch (e) {}
    }

    // 2. Try local fallback file
    if (!cfg) {
        try {
            const localPath = path.join(process.cwd(), 'fleet_config.json');
            if (fs.existsSync(localPath)) {
                const raw = fs.readFileSync(localPath, 'utf8');
                cfg = JSON.parse(raw);
            }
        } catch (e) {}
    }

    const merged = { ...DEFAULT_CONFIG, ...(cfg || {}) };

    // Dynamically populate latest_version_name and download_url from live GitHub release or telemetry if not explicitly set
    const ghRelease = await getLiveGitHubRelease();
    if (!merged.latest_version_name) {
        merged.latest_version_name = (ghRelease && ghRelease.version) || await getLiveFleetVersion();
    }
    if (!merged.download_url && ghRelease && ghRelease.download_url) {
        merged.download_url = ghRelease.download_url;
    }
    }

    return merged;
}

async function saveConfig(newConfig) {
    // 1. Save to KV if connected
    await callKv('SET', 'lloyd_fleet_config', JSON.stringify(newConfig));

    // 2. Best-effort local file save
    try {
        const localPath = path.join(process.cwd(), 'fleet_config.json');
        fs.writeFileSync(localPath, JSON.stringify(newConfig, null, 2), 'utf8');
    } catch (e) {}
}

module.exports = async function handler(req, res) {
    setCors(res);

    if (req.method === 'OPTIONS') {
        res.status(204).end();
        return;
    }

    try {
        if (req.method === 'GET') {
            const config = await getConfig();
            res.status(200).json(config);
            return;
        }

        if (req.method === 'POST') {
            let body = req.body;
            if (typeof body === 'string') {
                try { body = JSON.parse(body); } catch (e) {}
            }
            body = body || {};

            const current = await getConfig();
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

            await saveConfig(updated);
            res.status(200).json({ success: true, config: updated });
            return;
        }

        res.status(405).json({ error: 'Method not allowed' });
    } catch (err) {
        console.error('[API/CONFIG] Handler error:', err);
        res.status(200).json(DEFAULT_CONFIG);
    }
};
