/**
 * Audit Fixes — Unit & Handler Integration Tests
 * Covers C1 (write honesty), C3 (/api/config/admin), C7 (is_test tagging & purge),
 * C10 (semver sort), and E4 (/api/ota?sha256=true).
 * Runs with: node --test tests/audit_fixes.test.js
 */

'use strict';

const { test, describe, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

function createMockRes() {
    const res = {
        statusCode: 200,
        headers: {},
        body: null,
        ended: false,
        setHeader(k, v) { this.headers[k.toLowerCase()] = v; },
        writeHead(code, hdrs = {}) {
            this.statusCode = code;
            for (const [k, v] of Object.entries(hdrs)) {
                this.headers[k.toLowerCase()] = v;
            }
        },
        status(code) { this.statusCode = code; return this; },
        json(data) { this.body = data; this.ended = true; return this; },
        end(data) { if (data !== undefined) this.body = data; this.ended = true; return this; }
    };
    return res;
}

function loadFresh(relPath) {
    const abs = require.resolve(path.resolve(__dirname, '..', relPath));
    delete require.cache[abs];
    return require(abs);
}

describe('C1 — api/lib/db.js write honesty & 5xx caller responses', () => {
    let tmpDir;
    let origCwd;
    let origEnv;
    let origFetch;

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'lloyd-db-test-'));
        origCwd = process.cwd();
        process.chdir(tmpDir);
        origEnv = { ...process.env };
        origFetch = global.fetch;

        delete process.env.KV_REST_API_URL;
        delete process.env.KV_REST_API_TOKEN;
        delete process.env.UPSTASH_REDIS_REST_URL;
        delete process.env.UPSTASH_REDIS_REST_TOKEN;
        delete process.env.REDIS_URL;
        delete process.env.KV_URL;
        process.env.ADMIN_PASSWORD = 'test-secret-password';
    });

    afterEach(() => {
        process.chdir(origCwd);
        process.env = origEnv;
        global.fetch = origFetch;
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    test('local dev mode (no Redis configured) returns { ok: true } for all mutating methods', async () => {
        const db = loadFresh('api/lib/db.js');

        const cfgRes = await db.saveConfig({ min_version_code: 2 });
        assert.strictEqual(cfgRes.ok, true);
        assert.strictEqual(cfgRes.config.min_version_code, 2);

        const telRes = await db.upsertTelemetryRecord({ device_id: 'dev-1', app_version: '1.0.10' });
        assert.strictEqual(telRes.ok, true);
        assert.strictEqual(telRes.record.device_id, 'dev-1');

        const fbRes = await db.saveFeedback([{ id: 'fb1', message: 'hi', submitted_at: new Date().toISOString() }]);
        assert.strictEqual(fbRes.ok, true);
        assert.strictEqual(fbRes.feedback.length, 1);

        const delRes = await db.deleteTelemetryRecord('dev-1');
        assert.strictEqual(delRes.ok, true);
    });

    test('when Upstash REST is configured and returns HTTP 500, all mutating methods return { ok: false, error }', async () => {
        process.env.KV_REST_API_URL = 'https://mock-upstash.example.com';
        process.env.KV_REST_API_TOKEN = 'mock-token';
        global.fetch = async () => new Response('Redis write error', { status: 500 });

        const db = loadFresh('api/lib/db.js');

        const cfgRes = await db.saveConfig({ min_version_code: 99 });
        assert.strictEqual(cfgRes.ok, false);
        assert.ok(cfgRes.error.includes('500'));
        // Memory cache must not contain the failed write
        const loadedCfg = await db.getConfig();
        assert.notStrictEqual(loadedCfg.min_version_code, 99);

        const telRes = await db.upsertTelemetryRecord({ device_id: 'dev-fail' });
        assert.strictEqual(telRes.ok, false);
        assert.ok(telRes.error);

        const delRes = await db.deleteTelemetryRecord('dev-fail');
        assert.strictEqual(delRes.ok, false);

        const fbRes = await db.saveFeedback([{ id: 'fb-fail', message: 'x' }]);
        assert.strictEqual(fbRes.ok, false);
    });

    test('api/config.js, api/telemetry.js, api/feedback.js, and api/ban.js return 5xx when durable write fails', async () => {
        process.env.KV_REST_API_URL = 'https://mock-upstash.example.com';
        process.env.KV_REST_API_TOKEN = 'mock-token';
        global.fetch = async (url) => {
            // Let GET return empty/null, fail writes (POST to upstash)
            const strUrl = String(url);
            if (strUrl.includes('mock-upstash')) {
                return new Response(JSON.stringify({ error: 'WRONGTYPE or read-only' }), { status: 503 });
            }
            return new Response(JSON.stringify({}), { status: 404 });
        };

        loadFresh('api/lib/db.js');
        const configHandler = loadFresh('api/config.js');
        const telemetryHandler = loadFresh('api/telemetry.js');
        const feedbackHandler = loadFresh('api/feedback.js');
        const banHandler = loadFresh('api/ban.js');

        const authHeaders = { 'x-admin-password': 'test-secret-password' };

        // POST /api/config -> 503
        const resCfg = createMockRes();
        await configHandler({ method: 'POST', headers: authHeaders, body: { min_version_code: 3 } }, resCfg);
        assert.ok(resCfg.statusCode >= 500 && resCfg.statusCode < 600, `Expected 5xx, got ${resCfg.statusCode}`);

        // POST /api/telemetry -> 503
        const resTel = createMockRes();
        await telemetryHandler({ method: 'POST', headers: {}, body: { device_id: 'dev-123' } }, resTel);
        assert.ok(resTel.statusCode >= 500 && resTel.statusCode < 600, `Expected 5xx, got ${resTel.statusCode}`);

        // DELETE /api/telemetry -> 503
        const resDel = createMockRes();
        await telemetryHandler({ method: 'DELETE', headers: authHeaders, query: { device_id: 'dev-123' }, body: {} }, resDel);
        assert.ok(resDel.statusCode >= 500 && resDel.statusCode < 600, `Expected 5xx, got ${resDel.statusCode}`);

        // POST /api/feedback -> 503
        const resFb = createMockRes();
        await feedbackHandler({ method: 'POST', headers: {}, body: { message: 'Broken write test' } }, resFb);
        assert.ok(resFb.statusCode >= 500 && resFb.statusCode < 600, `Expected 5xx, got ${resFb.statusCode}`);

        // POST /api/ban -> 503
        const resBan = createMockRes();
        await banHandler({ method: 'POST', headers: authHeaders, body: { device_id: 'dev-123', action: 'ban' } }, resBan);
        assert.ok(resBan.statusCode >= 500 && resBan.statusCode < 600, `Expected 5xx, got ${resBan.statusCode}`);
    });
});

describe('C3 — GET /api/config/admin authentication & ban list projection', () => {
    let tmpDir;
    let origCwd;
    let origEnv;
    let origFetch;

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'lloyd-cfg-test-'));
        origCwd = process.cwd();
        process.chdir(tmpDir);
        origEnv = { ...process.env };
        origFetch = global.fetch;

        delete process.env.KV_REST_API_URL;
        delete process.env.KV_REST_API_TOKEN;
        delete process.env.UPSTASH_REDIS_REST_URL;
        delete process.env.UPSTASH_REDIS_REST_TOKEN;
        delete process.env.REDIS_URL;
        delete process.env.KV_URL;
        process.env.ADMIN_PASSWORD = 'test-secret-password';
        // Prevent live GitHub network calls during unit tests
        global.fetch = async () => new Response('Not Found', { status: 404 });
    });

    afterEach(() => {
        process.chdir(origCwd);
        process.env = origEnv;
        global.fetch = origFetch;
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    test('unauthenticated GET /api/config/admin returns 401; authenticated returns ban arrays', async () => {
        const db = loadFresh('api/lib/db.js');
        await db.saveConfig({
            min_version_code: 5,
            banned_devices: ['revoked-uuid-1'],
            banned_students: [10001]
        });

        const configHandler = loadFresh('api/config.js');

        // Unauthenticated admin GET -> 401
        const unauthRes = createMockRes();
        await configHandler({ method: 'GET', url: '/api/config/admin', query: { admin: '1' }, headers: {} }, unauthRes);
        assert.strictEqual(unauthRes.statusCode, 401);

        // Authenticated admin GET -> 200 with ban lists
        const adminRes = createMockRes();
        await configHandler({
            method: 'GET',
            url: '/api/config/admin',
            query: { admin: '1' },
            headers: { 'x-admin-password': 'test-secret-password' }
        }, adminRes);
        assert.strictEqual(adminRes.statusCode, 200);
        assert.deepStrictEqual(adminRes.body.banned_devices, ['revoked-uuid-1']);
        assert.deepStrictEqual(adminRes.body.banned_students, [10001]);

        // Public GET -> 200 WITHOUT ban lists
        const pubRes = createMockRes();
        await configHandler({ method: 'GET', url: '/api/config', query: {}, headers: {} }, pubRes);
        assert.strictEqual(pubRes.statusCode, 200);
        assert.strictEqual('banned_devices' in pubRes.body, false);
        assert.strictEqual('banned_students' in pubRes.body, false);
    });
});

describe('C7 — Telemetry is_test tagging & purgeTestTelemetry', () => {
    let tmpDir;
    let origCwd;
    let origEnv;

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'lloyd-tel-test-'));
        origCwd = process.cwd();
        process.chdir(tmpDir);
        origEnv = { ...process.env };

        delete process.env.KV_REST_API_URL;
        delete process.env.KV_REST_API_TOKEN;
        delete process.env.UPSTASH_REDIS_REST_URL;
        delete process.env.UPSTASH_REDIS_REST_TOKEN;
        delete process.env.REDIS_URL;
        delete process.env.KV_URL;
        process.env.ADMIN_PASSWORD = 'test-secret-password';
    });

    afterEach(() => {
        process.chdir(origCwd);
        process.env = origEnv;
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    test('POST stores is_test: true when provided and DELETE ?purge=test removes all is_test records', async () => {
        loadFresh('api/lib/db.js');
        const telemetryHandler = loadFresh('api/telemetry.js');
        const authHeaders = { 'x-admin-password': 'test-secret-password' };

        // Real device
        const realRes = createMockRes();
        await telemetryHandler({
            method: 'POST',
            headers: {},
            body: { device_id: 'real-pixel-uuid', app_version: '1.0.15' }
        }, realRes);
        assert.strictEqual(realRes.statusCode, 200);

        // Simulated test ping (even without 'test' in device_id)
        const testRes = createMockRes();
        await telemetryHandler({
            method: 'POST',
            headers: {},
            body: { device_id: 'simulated-ping-abc', app_version: '1.0.15', is_test: true }
        }, testRes);
        assert.strictEqual(testRes.statusCode, 200);

        // Verify stored tags
        const getBefore = createMockRes();
        await telemetryHandler({ method: 'GET', headers: authHeaders }, getBefore);
        assert.strictEqual(getBefore.statusCode, 200);
        assert.strictEqual(getBefore.body.length, 2);
        const testRec = getBefore.body.find(r => r.device_id === 'simulated-ping-abc');
        const realRec = getBefore.body.find(r => r.device_id === 'real-pixel-uuid');
        assert.strictEqual(testRec.is_test, true);
        assert.strictEqual(realRec.is_test, undefined);

        // Purge test records
        const purgeRes = createMockRes();
        await telemetryHandler({
            method: 'DELETE',
            headers: authHeaders,
            query: { purge: 'test' },
            body: {}
        }, purgeRes);
        assert.strictEqual(purgeRes.statusCode, 200);
        assert.strictEqual(purgeRes.body.purged_count, 1);
        assert.deepStrictEqual(purgeRes.body.purged, ['simulated-ping-abc']);

        // Verify only real device remains
        const getAfter = createMockRes();
        await telemetryHandler({ method: 'GET', headers: authHeaders }, getAfter);
        assert.strictEqual(getAfter.body.length, 1);
        assert.strictEqual(getAfter.body[0].device_id, 'real-pixel-uuid');
    });
});

describe('E4 — GET /api/ota?sha256=true', () => {
    let tmpDir;
    let origCwd;
    let origEnv;
    let origFetch;

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'lloyd-ota-test-'));
        origCwd = process.cwd();
        process.chdir(tmpDir);
        origEnv = { ...process.env };
        origFetch = global.fetch;

        delete process.env.KV_REST_API_URL;
        delete process.env.KV_REST_API_TOKEN;
        delete process.env.UPSTASH_REDIS_REST_URL;
        delete process.env.UPSTASH_REDIS_REST_TOKEN;
        delete process.env.REDIS_URL;
        delete process.env.KV_URL;
    });

    afterEach(() => {
        process.chdir(origCwd);
        process.env = origEnv;
        global.fetch = origFetch;
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    test('returns sha256 JSON from GitHub release .sha256 asset', async () => {
        global.fetch = async (url) => {
            if (String(url).includes('api.github.com')) {
                return new Response(JSON.stringify({
                    tag_name: 'v1.0.20',
                    assets: [
                        { name: 'LloydAttendance-v1.0.20.apk', browser_download_url: 'https://github.com/nkpcore/lloyd-erp/releases/download/v1.0.20/LloydAttendance-v1.0.20.apk' },
                        { name: 'LloydAttendance-v1.0.20.apk.sha256', browser_download_url: 'https://github.com/nkpcore/lloyd-erp/releases/download/v1.0.20/LloydAttendance-v1.0.20.apk.sha256' }
                    ]
                }), { status: 200 });
            }
            return new Response('deadbeef1234567890  LloydAttendance-v1.0.20.apk\n', { status: 200 });
        };

        loadFresh('api/lib/db.js');
        const otaHandler = loadFresh('api/ota.js');
        const res = createMockRes();
        await otaHandler({ method: 'GET', query: { sha256: 'true' }, headers: {} }, res);
        assert.strictEqual(res.statusCode, 200);
        assert.strictEqual(res.body.sha256, 'deadbeef1234567890');
        assert.strictEqual(res.body.version, '1.0.20');
    });
});

describe('C10 — Semver numeric sort in web/admin/app.js', () => {
    test('compareSemverDesc sorts 1.0.10 above 1.0.9 and 1.0.2', () => {
        const vm = require('node:vm');
        const appSrc = fs.readFileSync(path.resolve(__dirname, '..', 'web/admin/app.js'), 'utf8');
        // Extract parseSemver and compareSemverDesc functions
        const match = appSrc.match(/function parseSemver[\s\S]*?\r?\n\}\r?\n\r?\nfunction compareSemverDesc[\s\S]*?\r?\n\}/);
        assert.ok(match, 'compareSemverDesc helper must exist in web/admin/app.js');
        const ctx = {};
        vm.runInNewContext(match[0], ctx);

        const versions = ['1.0.2', '1.0.9', '1.0.10', '1.0.1', '2.0.0'];
        const sorted = [...versions].sort(ctx.compareSemverDesc);
        assert.deepStrictEqual(sorted, ['2.0.0', '1.0.10', '1.0.9', '1.0.2', '1.0.1']);
    });
});
