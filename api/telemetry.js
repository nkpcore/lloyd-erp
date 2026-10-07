/**
 * Lloyd ERP — Student Fleet Telemetry Ingestion API (Vercel Serverless / Cloud)
 * Supports Vercel KV / Upstash Redis with atomic HSET per device UUID.
 * Zero external npm dependencies. Pure standard library.
 */

const fs = require('fs');
const path = require('path');

// In-memory fallback if neither KV nor disk persistence is available
let memoryTelemetryStore = {};

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
        console.warn('[KV] Telemetry KV error:', e.message);
        return null;
    }
}

async function getRecords() {
    // 1. Try KV Hash
    const kvValues = await callKv('HVALS', 'lloyd_fleet_telemetry');
    if (Array.isArray(kvValues) && kvValues.length > 0) {
        const parsed = kvValues.map(v => {
            try { return typeof v === 'string' ? JSON.parse(v) : v; } catch (e) { return null; }
        }).filter(Boolean);
        parsed.sort((a, b) => new Date(b.timestamp || 0) - new Date(a.timestamp || 0));
        return parsed;
    }

    // 2. Try local file
    try {
        const localPath = path.join(process.cwd(), 'fleet_telemetry.json');
        if (fs.existsSync(localPath)) {
            const raw = fs.readFileSync(localPath, 'utf8');
            const data = JSON.parse(raw);
            const list = Array.isArray(data) ? data : Object.values(data);
            list.sort((a, b) => new Date(b.timestamp || 0) - new Date(a.timestamp || 0));
            return list;
        }
    } catch (e) {}

    // 3. Fallback to memory
    const memList = Object.values(memoryTelemetryStore);
    memList.sort((a, b) => new Date(b.timestamp || 0) - new Date(a.timestamp || 0));
    return memList;
}

async function upsertRecord(record) {
    if (!record || !record.device_id) return false;

    const payload = {
        ...record,
        server_received_at: new Date().toISOString()
    };

    // 1. Save to KV
    await callKv('HSET', 'lloyd_fleet_telemetry', record.device_id, JSON.stringify(payload));

    // 2. Update memory
    memoryTelemetryStore[record.device_id] = payload;

    // 3. Best-effort local file write
    try {
        const localPath = path.join(process.cwd(), 'fleet_telemetry.json');
        let diskStore = {};
        if (fs.existsSync(localPath)) {
            try {
                const raw = fs.readFileSync(localPath, 'utf8');
                const parsed = JSON.parse(raw);
                if (Array.isArray(parsed)) {
                    parsed.forEach(r => { if (r.device_id) diskStore[r.device_id] = r; });
                }
            } catch (e) {}
        }
        diskStore[record.device_id] = payload;
        fs.writeFileSync(localPath, JSON.stringify(Object.values(diskStore), null, 2), 'utf8');
    } catch (e) {}

    return true;
}

module.exports = async function handler(req, res) {
    setCors(res);

    if (req.method === 'OPTIONS') {
        res.status(204).end();
        return;
    }

    try {
        if (req.method === 'GET') {
            const records = await getRecords();
            res.status(200).json(records);
            return;
        }

        if (req.method === 'POST') {
            let body = req.body;
            if (typeof body === 'string') {
                try { body = JSON.parse(body); } catch (e) {}
            }
            body = body || {};

            if (!body.device_id) {
                res.status(400).json({ error: 'device_id is required' });
                return;
            }

            await upsertRecord(body);
            res.status(200).json({ success: true, timestamp: new Date().toISOString() });
            return;
        }

        res.status(405).json({ error: 'Method not allowed' });
    } catch (err) {
        console.error('[API/TELEMETRY] Handler error:', err);
        res.status(200).json([]);
    }
};
