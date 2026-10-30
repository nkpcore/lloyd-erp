/**
 * Lloyd ERP — Admin Authorization Helper
 * Uses crypto.timingSafeEqual for all comparisons (prevents timing attacks).
 * Stateless HMAC-SHA256 session token generation and verification.
 * Supports raw password fallback as well as cryptographic session tokens.
 */

const crypto = require('crypto');

const SESSION_TTL_MS = 8 * 60 * 60 * 1000; // 8 hours

function getSecret() {
    return (process.env.ADMIN_PASSWORD || 'lloyderp').trim();
}

/**
 * Constant-time string equality. Masks both value and length.
 * @param {string} a
 * @param {string} b
 * @returns {boolean}
 */
function safeEqual(a, b) {
    try {
        const ba = Buffer.from(String(a), 'utf8');
        const bb = Buffer.from(String(b), 'utf8');
        const lenMatch = ba.length === bb.length;
        const dummy = Buffer.alloc(ba.length);
        crypto.timingSafeEqual(ba, lenMatch ? bb : dummy);
        return lenMatch && crypto.timingSafeEqual(ba, bb);
    } catch (_) {
        return false;
    }
}

/**
 * Issues a stateless HMAC-signed session token.
 * Token format: timestamp.hmac(timestamp, secret)
 * @returns {string}
 */
function issueToken() {
    const secret = getSecret();
    const ts = Date.now().toString();
    const sig = crypto.createHmac('sha256', secret).update(ts).digest('hex');
    return `${ts}.${sig}`;
}

/**
 * Verifies if a given string is a valid, unexpired HMAC-signed session token.
 * @param {string} token
 * @returns {boolean}
 */
function isValidSession(token) {
    if (!token || typeof token !== 'string') return false;
    const parts = token.trim().split('.');
    if (parts.length !== 2) return false;
    const [ts, sig] = parts;
    const time = parseInt(ts, 10);
    if (isNaN(time) || Date.now() - time > SESSION_TTL_MS || time > Date.now() + 60000) {
        return false;
    }
    const secret = getSecret();
    const expectedSig = crypto.createHmac('sha256', secret).update(ts).digest('hex');
    return safeEqual(sig, expectedSig);
}

/**
 * Verify admin credentials.
 * Accepts either the raw admin password or a valid stateless session token.
 *
 * @param {object} req - HTTP request
 * @param {object|null} body - parsed request body
 * @returns {boolean}
 */
function verifyAdminAuth(req, body = null) {
    const expected = getSecret();
    if (!expected) return false;

    // 1. Authorization Bearer header
    const authHeader = req.headers['authorization'];
    if (authHeader) {
        const token = authHeader.replace(/^Bearer\s+/i, '').trim();
        if (safeEqual(token, expected) || isValidSession(token)) return true;
    }

    // 2. Custom admin-key header
    const adminKey = req.headers['x-admin-key'] || req.headers['x-admin-password'];
    if (adminKey) {
        const key = String(adminKey).trim();
        if (safeEqual(key, expected) || isValidSession(key)) return true;
    }

    // 3. Body field (for POST /auth or actions)
    const bodyKey = (body && (body.admin_key || body.admin_password || body.password))
        || (req.body && (req.body.admin_key || req.body.admin_password || req.body.password));
    if (bodyKey) {
        const key = String(bodyKey).trim();
        if (safeEqual(key, expected) || isValidSession(key)) return true;
    }

    return false;
}

module.exports = {
    verifyAdminAuth,
    issueToken,
    isValidSession,
    safeEqual
};
