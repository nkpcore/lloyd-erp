/**
 * Lloyd ERP — Admin Portal Authentication Endpoint
 * POST /api/auth — validates admin password, returns opaque session token.
 * Token is a random hex string, NOT the password itself.
 */

const crypto = require('crypto');
const { verifyAdminAuth } = require('./lib/auth');

// In-process session store. Serverless-safe: sessions expire per container restart
// (which is fine — admins re-login). Max 64 concurrent sessions.
const SESSION_TTL_MS = 8 * 60 * 60 * 1000; // 8 hours
const sessions = new Map(); // token -> { expiresAt }

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-admin-key, x-admin-password, Accept');
    res.setHeader('Cache-Control', 'no-store');
}

function issueToken() {
    const token = crypto.randomBytes(32).toString('hex');
    sessions.set(token, { expiresAt: Date.now() + SESSION_TTL_MS });
    // Evict expired sessions (cap memory)
    if (sessions.size > 64) {
        for (const [t, s] of sessions) {
            if (s.expiresAt < Date.now()) sessions.delete(t);
        }
    }
    return token;
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
                token,           // opaque random token, NOT the password
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

// Exported so other routes can validate session tokens
module.exports.isValidSession = function (token) {
    if (!token) return false;
    const s = sessions.get(String(token).trim());
    if (!s) return false;
    if (s.expiresAt < Date.now()) { sessions.delete(token); return false; }
    return true;
};
