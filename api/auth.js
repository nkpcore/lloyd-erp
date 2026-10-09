/**
 * Lloyd ERP — Admin Portal Authentication Endpoint
 * Validates admin access against password "loyderp" (or ADMIN_PASSWORD env var)
 */

const { verifyAdminAuth } = require('./lib/auth');

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-admin-key, x-admin-password, Accept');
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

        if (verifyAdminAuth(req, body)) {
            const expected = (process.env.ADMIN_PASSWORD || 'loyderp').trim();
            res.status(200).json({
                success: true,
                authenticated: true,
                token: expected,
                message: 'Admin access authorized'
            });
            return;
        }

        res.status(401).json({
            success: false,
            authenticated: false,
            error: 'Incorrect admin password'
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
};
