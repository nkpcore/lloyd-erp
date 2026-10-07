/**
 * Lloyd ERP — Fleet Governance Configuration API (Vercel Serverless / Cloud)
 * Supports Vercel KV / Upstash Redis with zero external npm dependencies.
 * Falls back to bundled configuration gracefully if KV is not yet connected.
 */

const fs = require('fs');
const path = require('path');

const DEFAULT_CONFIG = {
    min_version_code: 15,
    latest_version_name: 'v1.0.15',
    download_url: 'https://github.com/nkpcore/lloyd-erp/releases/latest',
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

async function getConfig() {
    // 1. Try Vercel KV / Upstash Redis
    const kvData = await callKv('GET', 'lloyd_fleet_config');
    if (kvData) {
        try {
            return typeof kvData === 'string' ? JSON.parse(kvData) : kvData;
        } catch (e) {}
    }

    // 2. Try local fallback file
    try {
        const localPath = path.join(process.cwd(), 'fleet_config.json');
        if (fs.existsSync(localPath)) {
            const raw = fs.readFileSync(localPath, 'utf8');
            return JSON.parse(raw);
        }
    } catch (e) {}

    return DEFAULT_CONFIG;
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
                min_version_code: parseInt(body.min_version_code) || current.min_version_code,
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
