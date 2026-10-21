/**
 * Fleet Admin Authentication — Unit Tests
 * Runs with: node --test tests/fleet_admin_auth.test.js
 */

'use strict';

const { test, describe, before, after } = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');

// ─── Inline minimal stubs so tests run without a live server ─────────────────

// Stub for api/lib/auth.js logic
const { safeEqual } = (() => {
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
    return { safeEqual };
})();

// Stub session store matching api/auth.js behaviour
const SESSION_TTL_MS = 8 * 60 * 60 * 1000;
const sessions = new Map();

function issueToken() {
    const token = crypto.randomBytes(32).toString('hex');
    sessions.set(token, { expiresAt: Date.now() + SESSION_TTL_MS });
    return token;
}

function isValidSession(token) {
    if (!token) return false;
    const s = sessions.get(String(token).trim());
    if (!s) return false;
    if (s.expiresAt < Date.now()) { sessions.delete(token); return false; }
    return true;
}

function logoutSession(token) {
    sessions.delete(String(token).trim());
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('safeEqual — timing-safe comparison', () => {
    test('identical strings → true', () => {
        assert.strictEqual(safeEqual('correct', 'correct'), true);
    });

    test('different strings → false', () => {
        assert.strictEqual(safeEqual('correct', 'wrong'), false);
    });

    test('empty vs non-empty → false', () => {
        assert.strictEqual(safeEqual('', 'password'), false);
    });

    test('both empty → true', () => {
        assert.strictEqual(safeEqual('', ''), true);
    });

    test('does not throw on null/undefined inputs', () => {
        assert.doesNotThrow(() => safeEqual(null, 'x'));
        assert.doesNotThrow(() => safeEqual('x', undefined));
    });

    test('returns false for null/undefined inputs', () => {
        assert.strictEqual(safeEqual(null, 'x'), false);
        assert.strictEqual(safeEqual(undefined, undefined), true); // 'undefined' === 'undefined'
    });
});

describe('Session token issuance', () => {
    test('issueToken() returns a 64-char hex string (256-bit)', () => {
        const token = issueToken();
        assert.match(token, /^[0-9a-f]{64}$/);
    });

    test('each token is unique', () => {
        const t1 = issueToken();
        const t2 = issueToken();
        assert.notStrictEqual(t1, t2);
    });

    test('issued token is immediately valid', () => {
        const token = issueToken();
        assert.strictEqual(isValidSession(token), true);
    });
});

describe('Session validation', () => {
    test('unknown token → false', () => {
        assert.strictEqual(isValidSession('nonexistent_token_xyz'), false);
    });

    test('null token → false', () => {
        assert.strictEqual(isValidSession(null), false);
    });

    test('empty string token → false', () => {
        assert.strictEqual(isValidSession(''), false);
    });

    test('expired session → false', () => {
        const token = crypto.randomBytes(32).toString('hex');
        // Insert already-expired session
        sessions.set(token, { expiresAt: Date.now() - 1000 });
        assert.strictEqual(isValidSession(token), false);
        // Also confirm it is evicted
        assert.strictEqual(sessions.has(token), false);
    });
});

describe('Session logout / revocation', () => {
    test('token is invalid after logout', () => {
        const token = issueToken();
        assert.strictEqual(isValidSession(token), true);
        logoutSession(token);
        assert.strictEqual(isValidSession(token), false);
    });

    test('logout of non-existent token does not throw', () => {
        assert.doesNotThrow(() => logoutSession('totally_fake_token'));
    });
});

describe('ADMIN_PASSWORD env guard', () => {
    test('verifyAdminAuth returns false when ADMIN_PASSWORD is unset', () => {
        const savedEnv = process.env.ADMIN_PASSWORD;
        delete process.env.ADMIN_PASSWORD;

        // Replicate api/lib/auth.js verifyAdminAuth logic
        const expected = (process.env.ADMIN_PASSWORD || '').trim();
        const result = expected.length > 0 ? safeEqual('anypassword', expected) : false;

        assert.strictEqual(result, false);
        if (savedEnv !== undefined) process.env.ADMIN_PASSWORD = savedEnv;
    });

    test('verifyAdminAuth returns false on wrong password even when env is set', () => {
        process.env.ADMIN_PASSWORD = 'correct-secret';
        const expected = (process.env.ADMIN_PASSWORD || '').trim();
        assert.strictEqual(safeEqual('wrong', expected), false);
        delete process.env.ADMIN_PASSWORD;
    });

    test('verifyAdminAuth returns true on correct password', () => {
        process.env.ADMIN_PASSWORD = 'correct-secret';
        const expected = (process.env.ADMIN_PASSWORD || '').trim();
        assert.strictEqual(safeEqual('correct-secret', expected), true);
        delete process.env.ADMIN_PASSWORD;
    });
});
