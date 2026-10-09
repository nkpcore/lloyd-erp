/**
 * Lloyd ERP — Admin Authorization Helper
 * Validates admin access against password "loyderp" (or ADMIN_PASSWORD env var)
 */

function verifyAdminAuth(req, body = null) {
    const expected = (process.env.ADMIN_PASSWORD || 'loyderp').trim();
    
    // 1. Check custom headers
    const adminKey = req.headers['x-admin-key'] || req.headers['x-admin-password'];
    if (adminKey && adminKey.trim() === expected) return true;

    // 2. Check Authorization header
    const authHeader = req.headers['authorization'];
    if (authHeader) {
        const token = authHeader.replace(/^Bearer\s+/i, '').trim();
        if (token === expected) return true;
    }

    // 3. Check query param
    const queryKey = req.query?.admin_key || req.query?.key || req.query?.password;
    if (queryKey && queryKey.trim() === expected) return true;

    // 4. Check parsed body
    const bodyKey = body?.admin_key || body?.admin_password || body?.password || req.body?.admin_key || req.body?.admin_password || req.body?.password;
    if (bodyKey && String(bodyKey).trim() === expected) return true;

    return false;
}

module.exports = {
    verifyAdminAuth
};
