/**
 * Lloyd ERP — Admin Portal Authentication Endpoint
 * POST /api/auth — validates admin password, returns opaque session token.
 * Token is a stateless HMAC-signed token, NOT the password itself.
 */

const { verifyAdminAuth, issueToken, isValidSession } = require('./lib/auth');

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-admin-key, x-admin-password, Accept');
    res.setHeader('Cache-Control', 'no-store');
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
            const token = issueToken();
            res.status(200).json({
                success: true,
                authenticated: true,
                token,           // stateless HMAC-signed token
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
        res.status(500).json({ error: 'Internal server error' });
    }
};

module.exports.isValidSession = isValidSession;
