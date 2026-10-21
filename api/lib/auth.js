/**
 * Lloyd ERP — Admin Authorization Helper
 * Uses crypto.timingSafeEqual for all comparisons (prevents timing attacks).
 * NEVER fallback to a hardcoded password. No ADMIN_PASSWORD env = deny all.
 */

const crypto = require('crypto');

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
        // Always run timingSafeEqual to avoid short-circuit timing leak on length mismatch.
        // Compare against a dummy same-length buffer when lengths differ.
        const lenMatch = ba.length === bb.length;
        const dummy = Buffer.alloc(ba.length);
        crypto.timingSafeEqual(ba, lenMatch ? bb : dummy);
        return lenMatch && crypto.timingSafeEqual(ba, bb);
    } catch (_) {
        return false;
    }
}

/**
 * Verify admin credentials. Used ONLY by POST /api/auth (login).
 * Checks Authorization Bearer or x-admin-key header or body field.
 * Returns false if ADMIN_PASSWORD env is not set.
 *
 * @param {object} req - HTTP request
 * @param {object|null} body - parsed request body
 * @returns {boolean}
 */
function verifyAdminAuth(req, body = null) {
    const expected = (process.env.ADMIN_PASSWORD || '').trim();
    if (!expected) return false; // No password configured = deny all

    // 1. Authorization Bearer header
    const authHeader = req.headers['authorization'];
    if (authHeader) {
        const token = authHeader.replace(/^Bearer\s+/i, '').trim();
        if (safeEqual(token, expected)) return true;
    }

    // 2. Custom admin-key header
    const adminKey = req.headers['x-admin-key'] || req.headers['x-admin-password'];
    if (adminKey && safeEqual(adminKey.trim(), expected)) return true;

    // 3. Body field (for POST /auth only — body is the credential payload)
    const bodyKey = (body && (body.admin_key || body.admin_password || body.password))
        || (req.body && (req.body.admin_key || req.body.admin_password || req.body.password));
    if (bodyKey && safeEqual(String(bodyKey).trim(), expected)) return true;

    return false;
}

module.exports = {
    verifyAdminAuth,
    safeEqual
};
