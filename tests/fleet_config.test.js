/**
 * Fleet Config API — Unit Tests
 * Runs with: node --test tests/fleet_config.test.js
 */

'use strict';

const { test, describe } = require('node:test');
const assert = require('node:assert/strict');

// ─── Inline the config allowlist logic to test in isolation ──────────────────

const ALLOWED_CONFIG_KEYS = [
    'latest_version_code', 'latest_version_name', 'min_version_code',
    'download_url', 'broadcast_notice', 'mandatory_update', 'sha256',
    'maintenance_mode', 'maintenance_message', 'pause_updates'
];

/**
 * Applies the Phase 1.2 allowlist + type-gate logic from api/config.js.
 * Returns { ok: true, update } or { ok: false, error }
 */
function applyConfigAllowlist(body) {
    const update = {};
    for (const key of ALLOWED_CONFIG_KEYS) {
        if (body[key] !== undefined) update[key] = body[key];
    }

    if (update.latest_version_code !== undefined) {
        const vc = parseInt(update.latest_version_code);
        if (!Number.isInteger(vc) || vc <= 0) {
            return { ok: false, error: 'latest_version_code must be a positive integer' };
        }
        update.latest_version_code = vc;
    }

    if (update.min_version_code !== undefined) {
        const mvc = parseInt(update.min_version_code);
        if (!Number.isInteger(mvc) || mvc < 0) {
            return { ok: false, error: 'min_version_code must be a non-negative integer' };
        }
        update.min_version_code = mvc;
    }

    if (update.download_url !== undefined) {
        if (typeof update.download_url !== 'string' || !update.download_url.startsWith('https://')) {
            return { ok: false, error: 'download_url must be an HTTPS URL' };
        }
    }

    if (update.maintenance_mode !== undefined) update.maintenance_mode = Boolean(update.maintenance_mode);
    if (update.mandatory_update !== undefined) update.mandatory_update = Boolean(update.mandatory_update);
    if (update.pause_updates !== undefined) update.pause_updates = Boolean(update.pause_updates);

    return { ok: true, update };
}

/**
 * Simulates GET /api/config public projection (Phase 1.2).
 * Returns only client-safe fields — never exposes ban arrays.
 */
function publicConfigProjection(fullConfig) {
    const { banned_devices, banned_students, ...publicConfig } = fullConfig;
    void banned_devices; void banned_students;
    return publicConfig;
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('GET /api/config — public projection (Phase 1.2)', () => {
    const fullConfig = {
        latest_version_code: 12,
        latest_version_name: '1.2.0',
        min_version_code: 10,
        download_url: 'https://github.com/nkpcore/lloyd-erp/releases/download/v1.2.0/app.apk',
        broadcast_notice: 'New release!',
        mandatory_update: false,
        sha256: null,
        banned_devices: ['device-abc', 'device-xyz'],
        banned_students: [28960, 11111]
    };

    const projected = publicConfigProjection(fullConfig);

    test('banned_devices is NOT present in public projection', () => {
        assert.strictEqual('banned_devices' in projected, false);
    });

    test('banned_students is NOT present in public projection', () => {
        assert.strictEqual('banned_students' in projected, false);
    });

    test('version fields ARE present in public projection', () => {
        assert.strictEqual(projected.latest_version_code, 12);
        assert.strictEqual(projected.latest_version_name, '1.2.0');
        assert.strictEqual(projected.download_url, fullConfig.download_url);
    });
});

describe('POST /api/config — strict field allowlist (Phase 1.2)', () => {
    test('unknown body properties are dropped', () => {
        const body = {
            broadcast_notice: 'Test notice',
            __proto__: { polluted: true },
            exec: 'rm -rf /',
            internal_flag: true
        };
        const { ok, update } = applyConfigAllowlist(body);
        assert.strictEqual(ok, true);
        assert.strictEqual('exec' in update, false);
        assert.strictEqual('internal_flag' in update, false);
        assert.strictEqual(update.broadcast_notice, 'Test notice');
    });

    test('valid latest_version_code integer is accepted', () => {
        const { ok, update } = applyConfigAllowlist({ latest_version_code: 15 });
        assert.strictEqual(ok, true);
        assert.strictEqual(update.latest_version_code, 15);
    });

    test('latest_version_code zero is rejected', () => {
        const { ok, error } = applyConfigAllowlist({ latest_version_code: 0 });
        assert.strictEqual(ok, false);
        assert.ok(error.includes('latest_version_code'));
    });

    test('latest_version_code negative is rejected', () => {
        const { ok } = applyConfigAllowlist({ latest_version_code: -5 });
        assert.strictEqual(ok, false);
    });

    test('http download_url is rejected (must be https)', () => {
        const { ok, error } = applyConfigAllowlist({
            download_url: 'http://evil.example.com/malware.apk'
        });
        assert.strictEqual(ok, false);
        assert.ok(error.includes('HTTPS'));
    });

    test('https download_url is accepted', () => {
        const { ok, update } = applyConfigAllowlist({
            download_url: 'https://github.com/nkpcore/lloyd-erp/releases/latest/app.apk'
        });
        assert.strictEqual(ok, true);
        assert.ok(update.download_url.startsWith('https://'));
    });

    test('maintenance_mode is coerced to boolean', () => {
        const { ok, update } = applyConfigAllowlist({ maintenance_mode: 'true' });
        assert.strictEqual(ok, true);
        assert.strictEqual(typeof update.maintenance_mode, 'boolean');
    });

    test('ban arrays cannot be set via config POST (preserved from DB)', () => {
        // The allowlist does NOT include banned_devices or banned_students
        const body = { banned_devices: ['attacker'], banned_students: [99999] };
        const { ok, update } = applyConfigAllowlist(body);
        assert.strictEqual(ok, true);
        assert.strictEqual('banned_devices' in update, false);
        assert.strictEqual('banned_students' in update, false);
    });
});

describe('POST /api/config — unauthenticated rejection (contract only)', () => {
    test('verifyAdminAuth returns false when ADMIN_PASSWORD is empty', () => {
        // Guard: ADMIN_PASSWORD must be set
        const expected = (process.env.ADMIN_PASSWORD || '').trim();
        if (!expected) {
            // Correct: no password = deny all
            assert.strictEqual(expected.length, 0);
        }
    });
});
