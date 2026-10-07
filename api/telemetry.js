/**
 * Lloyd ERP — Student Fleet Telemetry Ingestion API (Vercel Serverless / Cloud)
 * Uses api/lib/db.js for instant non-hanging persistence with Redis and memory/disk fallbacks.
 */

const db = require('./lib/db');

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept');
}

module.exports = async function handler(req, res) {
    setCors(res);

    if (req.method === 'OPTIONS') {
        res.status(204).end();
        return;
    }

    try {
        if (req.method === 'GET') {
            const records = await db.getTelemetryRecords();
            res.status(200).json(records);
            return;
        }

        if (req.method === 'POST') {
            let body = req.body;
            if (typeof body === 'string') {
                try { body = JSON.parse(body); } catch (_) {}
            }
            body = body || {};

            if (!body.device_id) {
                res.status(400).json({ error: 'device_id is required' });
                return;
            }

            await db.upsertTelemetryRecord(body);
            res.status(200).json({ success: true, timestamp: new Date().toISOString() });
            return;
        }

        res.status(405).json({ error: 'Method not allowed' });
    } catch (err) {
        console.error('[API/TELEMETRY] Handler error:', err);
        res.status(200).json([]);
    }
};
