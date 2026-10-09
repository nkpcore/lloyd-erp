/**
 * Lloyd ERP — 1-Click Targeted Device & Student Revocation API (Vercel Serverless / Cloud)
 * Uses api/lib/db.js for unified persistence across Redis, disk, and in-memory caches.
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

    if (req.method !== 'POST') {
        res.status(405).json({ error: 'Method not allowed' });
        return;
    }

    try {
        let body = req.body;
        if (typeof body === 'string') {
            try { body = JSON.parse(body); } catch (_) {}
        }
        body = body || {};

        const deviceId = (body.device_id || '').trim();
        const studentId = body.student_id ? parseInt(body.student_id) : null;

        if (!deviceId && !studentId) {
            res.status(400).json({ error: 'device_id or student_id is required' });
            return;
        }

        const config = await db.getConfig();
        let isDeviceBanned = false;
        let isStudentBanned = false;

        const action = (body.action || 'toggle').toLowerCase();

        if (deviceId) {
            const bans = new Set((config.banned_devices || []).map(d => String(d).trim()));
            if (action === 'ban') {
                bans.add(deviceId);
                isDeviceBanned = true;
            } else if (action === 'unban') {
                bans.delete(deviceId);
                isDeviceBanned = false;
            } else { // toggle
                if (bans.has(deviceId)) {
                    bans.delete(deviceId);
                    isDeviceBanned = false;
                } else {
                    bans.add(deviceId);
                    isDeviceBanned = true;
                }
            }
            config.banned_devices = Array.from(bans);
        }

        if (studentId) {
            const studentBans = new Set((config.banned_students || []).map(s => parseInt(s)).filter(Boolean));
            if (action === 'ban') {
                studentBans.add(studentId);
                isStudentBanned = true;
            } else if (action === 'unban') {
                studentBans.delete(studentId);
                isStudentBanned = false;
            } else { // toggle
                if (studentBans.has(studentId)) {
                    studentBans.delete(studentId);
                    isStudentBanned = false;
                } else {
                    studentBans.add(studentId);
                    isStudentBanned = true;
                }
            }
            config.banned_students = Array.from(studentBans);
        }

        const saved = await db.saveConfig(config);

        res.status(200).json({
            success: true,
            device_id: deviceId || null,
            student_id: studentId || null,
            is_device_banned: isDeviceBanned,
            is_student_banned: isStudentBanned,
            banned_devices: saved.banned_devices || [],
            banned_students: saved.banned_students || []
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
};
