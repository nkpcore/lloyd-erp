/**
 * Lloyd ERP — 1-Click Targeted Device Ban/Unban API (Vercel Serverless / Cloud)
 */

const fs = require('fs');
const path = require('path');

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept');
}

function getKvCredentials() {
    let url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
    let token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
    if ((!url || !token) && (process.env.REDIS_URL || process.env.KV_URL)) {
        try {
            const raw = process.env.REDIS_URL || process.env.KV_URL;
            const parsed = new URL(raw);
            url = `https://${parsed.hostname}`;
            token = decodeURIComponent(parsed.password || parsed.username || '');
        } catch (_) {}
    }
    return { url, token };
}

async function callKv(command, ...args) {
    const { url, token } = getKvCredentials();
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
        return null;
    }
}

async function getConfig() {
    const kvData = await callKv('GET', 'lloyd_fleet_config');
    if (kvData) {
        try {
            return typeof kvData === 'string' ? JSON.parse(kvData) : kvData;
        } catch (e) {}
    }
    try {
        const localPath = path.join(process.cwd(), 'fleet_config.json');
        if (fs.existsSync(localPath)) {
            return JSON.parse(fs.readFileSync(localPath, 'utf8'));
        }
    } catch (e) {}
    return { banned_devices: [] };
}

async function saveConfig(cfg) {
    await callKv('SET', 'lloyd_fleet_config', JSON.stringify(cfg));
    try {
        const localPath = path.join(process.cwd(), 'fleet_config.json');
        fs.writeFileSync(localPath, JSON.stringify(cfg, null, 2), 'utf8');
    } catch (e) {}
}

module.exports = async function handler(req, res) {
    setCors(res);

    if (req.method === 'OPTIONS') {
        res.status(204).end();
        return;
    }

    if (req.method !== 'POST') {
        res.status(405).json({ error: 'Method not allowed' });
        return;
    }

    try {
        let body = req.body;
        if (typeof body === 'string') {
            try { body = JSON.parse(body); } catch (e) {}
        }
        body = body || {};

        const deviceId = (body.device_id || '').trim();
        if (!deviceId) {
            res.status(400).json({ error: 'device_id is required' });
            return;
        }

        const config = await getConfig();
        const bans = new Set(config.banned_devices || []);
        let isBanned = false;

        if (bans.has(deviceId)) {
            bans.delete(deviceId);
            isBanned = false;
        } else {
            bans.add(deviceId);
            isBanned = true;
        }

        config.banned_devices = Array.from(bans);
        await saveConfig(config);

        res.status(200).json({
            success: true,
            device_id: deviceId,
            is_banned: isBanned,
            total_banned: config.banned_devices.length
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
};
