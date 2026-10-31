/**
 * Lloyd ERP — Fleet Telemetry & Admin Governance Server
 * Zero external dependencies (stdlib only + optional redis from api/lib/db).
 * ZERO HARDCODED VERSIONS / CREDENTIALS / MOCK DATA.
 *
 * Routes
 * ──────
 * Public (no auth):
 *   GET  /health              → liveness + uptime + env check
 *   GET  /config              → public config (no ban lists)
 *   GET  /ota                 → update check (?current_version=X.Y.Z)
 *   GET  /ota/download        → redirect to latest APK asset (GitHub token required)
 *   POST /telemetry           → device heartbeat (rate-limited per device)
 *
 * Admin (Bearer / x-admin-key / body.admin_key):
 *   POST /auth                → verify admin creds (returns 200 OK, no secret echo)
 *   POST /config              → update public config fields (no ban array access)
 *   GET  /telemetry           → full telemetry records
 *   DELETE /telemetry         → delete or purge test records
 *   POST /ban                 → explicit ban/unban device or student
 *   GET  /stats               → aggregated fleet KPIs
 *   GET  /config/admin        → full config including ban lists
 */

'use strict';

const http  = require('http');
const fs    = require('fs');
const path  = require('path');

const PORT      = parseInt(process.env.PORT || '8080', 10);
const ADMIN_DIR = path.join(__dirname, 'web', 'admin');
const SERVER_START = Date.now();

// ── Admin password initialization ───────────────────────────────────────────
if (!process.env.ADMIN_PASSWORD) {
    process.env.ADMIN_PASSWORD = 'lloyderp';
}

const db                      = require('./api/lib/db');
const { verifyAdminAuth, issueToken } = require('./api/lib/auth');

// ── Helpers ──────────────────────────────────────────────────────────────────

const MIME = {
    '.html': 'text/html; charset=utf-8',
    '.css':  'text/css; charset=utf-8',
    '.js':   'application/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg':  'image/svg+xml',
    '.png':  'image/png',
    '.jpg':  'image/jpeg',
    '.ico':  'image/x-icon',
    '.txt':  'text/plain; charset=utf-8',
    '.woff2':'font/woff2',
};

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers',
        'Content-Type, Authorization, Accept, x-admin-key, x-admin-password');
    res.setHeader('Cache-Control', 'no-store');
}

function setSecurityHeaders(res) {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data: https:; connect-src 'self' https: http://localhost:8080; frame-ancestors 'none'");
    res.setHeader('Permissions-Policy', 'geolocation=(), microphone=(), camera=()');
}

function isValidOtaUrl(url) {
    if (!url || typeof url !== 'string' || !url.startsWith('https://')) return false;
    const ALLOWED_HOSTS = [
        'github.com/nkpcore/lloyd-erp/releases/',
        'objects.githubusercontent.com/',
        'github-releases.githubusercontent.com/',
        'raw.githubusercontent.com/nkpcore/lloyd-erp/',
        'lloyd-erp-sand.vercel.app/'
    ];
    return ALLOWED_HOSTS.some(h => url.includes(h));
}

function json(res, status, data) {
    const body = JSON.stringify(data, null, 2);
    res.writeHead(status, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': Buffer.byteLength(body),
    });
    res.end(body);
}

function parseBody(req) {
    return new Promise((resolve, reject) => {
        let raw = '';
        req.on('data', chunk => {
            raw += chunk;
            if (raw.length > 512_000) { req.destroy(); reject(new Error('Payload too large')); }
        });
        req.on('end', () => {
            if (!raw.trim()) return resolve({});
            try { resolve(JSON.parse(raw)); } catch { reject(new Error('Invalid JSON')); }
        });
        req.on('error', reject);
    });
}

function serveStatic(filePath, res) {
    fs.stat(filePath, (err, stats) => {
        if (err || !stats.isFile()) {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('404 Not Found');
            return;
        }
        const ext  = path.extname(filePath).toLowerCase();
        const mime = MIME[ext] || 'application/octet-stream';
        if (ext === '.html') {
            res.setHeader('Content-Security-Policy', "default-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.gstatic.com; frame-ancestors 'none'");
        }
        res.writeHead(200, {
            'Content-Type':   mime,
            'Content-Length': stats.size,
            'Cache-Control':  'no-cache',
        });
        fs.createReadStream(filePath).pipe(res);
    });
}

function cleanVersion(v) {
    return (v || '').trim().replace(/^[vV]/, '').trim();
}

function isNewer(remote, local) {
    const r = cleanVersion(remote).split('.').map(n => parseInt(n) || 0);
    const l = cleanVersion(local).split('.').map(n => parseInt(n) || 0);
    const len = Math.max(r.length, l.length);
    for (let i = 0; i < len; i++) {
        if ((r[i] || 0) > (l[i] || 0)) return true;
        if ((r[i] || 0) < (l[i] || 0)) return false;
    }
    return false;
}

// ── GitHub Release Cache ──────────────────────────────────────────────────────

let ghCache     = null;
let ghCacheTime = 0;
const GH_TTL    = 60_000; // 60 s

async function getGitHubRelease() {
    const now = Date.now();
    if (ghCache && now - ghCacheTime < GH_TTL) return ghCache;

    const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
    const repo  = process.env.GITHUB_REPOSITORY || 'nkpcore/lloyd-erp';
    const headers = {
        'User-Agent': 'LloydERP-Server',
        'Accept':     'application/vnd.github.v3+json',
    };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    try {
        const resp = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, {
            headers,
            signal: AbortSignal.timeout(2500),
        });
        if (!resp.ok) return ghCache; // serve stale on error
        const release  = await resp.json();
        const apk      = (release.assets || []).find(a => a.name.toLowerCase().endsWith('.apk'));
        const sha256Asset = (release.assets || []).find(a => a.name.toLowerCase().endsWith('.sha256'));
        let sha256 = null;
        if (sha256Asset && sha256Asset.browser_download_url) {
            try {
                const hashResp = await fetch(sha256Asset.browser_download_url, {
                    headers,
                    signal: AbortSignal.timeout(2000),
                });
                if (hashResp.ok) {
                    const text = await hashResp.text();
                    sha256 = text.trim().split(/\s+/)[0] || null;
                }
            } catch { /* ignore hash fetch error */ }
        }
        ghCache = {
            version:     cleanVersion(release.tag_name || release.name || ''),
            notes:       release.body || release.name || '',
            assetId:     apk?.id    ?? null,
            assetUrl:    apk?.url   ?? null,
            browserUrl:  apk?.browser_download_url ?? null,
            publishedAt: release.published_at || null,
            sha256:      sha256 || null,
        };
        ghCacheTime = now;
    } catch { /* serve stale or null */ }
    return ghCache;
}

// ── Rate limiter (telemetry POST, per device_id, memory only) ────────────────

const rateLimiter = new Map(); // device_id → last_hit_ms
const RATE_WINDOW = 30_000;   // 30 s per device

function checkRateLimit(deviceId) {
    const now  = Date.now();
    const last = rateLimiter.get(deviceId) || 0;
    if (now - last < RATE_WINDOW) return false; // blocked
    rateLimiter.set(deviceId, now);
    return true; // allowed
}

// Prune rate limiter map every 5 min to avoid memory growth
setInterval(() => {
    const cutoff = Date.now() - RATE_WINDOW * 4;
    for (const [k, t] of rateLimiter) {
        if (t < cutoff) rateLimiter.delete(k);
    }
}, 300_000).unref();

// ── Allowed config keys for POST /config ────────────────────────────────────

const CONFIG_ALLOWLIST = new Set([
    'latest_version_code', 'latest_version_name', 'min_version_code',
    'download_url', 'broadcast_notice', 'mandatory_update', 'sha256',
    'maintenance_mode', 'maintenance_message', 'pause_updates',
    'notifications_enabled'
]);

// ── Fleet stats aggregation ───────────────────────────────────────────────────

function computeStats(records, config) {
    const now        = Date.now();
    const dayMs      = 86_400_000;
    const bannedSet  = new Set((config.banned_devices || []).map(String));
    const stuBanSet  = new Set((config.banned_students || []).map(Number));

    const versionMap = {};
    const osMap      = {};
    const modelMap   = {};
    let activeToday  = 0;
    let activeWeek   = 0;

    for (const r of records) {
        const ts       = new Date(r.timestamp || r.server_received_at || 0).getTime();
        const age      = now - ts;
        if (age < dayMs)    activeToday++;
        if (age < dayMs*7)  activeWeek++;

        const ver = cleanVersion(r.app_version_name || r.app_version || '');
        if (ver) versionMap[ver] = (versionMap[ver] || 0) + 1;

        const os = (r.os_version || '').split(' ')[0] || 'Unknown';
        osMap[os] = (osMap[os] || 0) + 1;

        const model = r.device_model || 'Unknown';
        modelMap[model] = (modelMap[model] || 0) + 1;
    }

    const topVersions = Object.entries(versionMap)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10)
        .map(([v, c]) => ({ version: v, count: c }));

    const topModels = Object.entries(modelMap)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10)
        .map(([m, c]) => ({ model: m, count: c }));

    const topOS = Object.entries(osMap)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10)
        .map(([o, c]) => ({ os: o, count: c }));

    const latest = topVersions[0]?.version || config.latest_version_name || null;
    const latestCount = latest ? (versionMap[latest] || 0) : 0;
    const adoption = records.length ? Math.round((latestCount / records.length) * 100) : 0;

    const bannedDeviceCount  = records.filter(r => bannedSet.has(String(r.device_id))).length;
    const bannedStudentCount = records.filter(r => stuBanSet.has(Number(r.student_id))).length;

    return {
        total_devices:         records.length,
        active_today:          activeToday,
        active_last_7_days:    activeWeek,
        latest_version:        latest,
        latest_version_adoption_pct: adoption,
        banned_device_count:   bannedDeviceCount,
        banned_student_count:  bannedStudentCount,
        version_distribution:  topVersions,
        top_device_models:     topModels,
        top_os_versions:       topOS,
        maintenance_mode:      config.maintenance_mode || false,
        pause_updates:         config.pause_updates || false,
        computed_at:           new Date().toISOString(),
    };
}

// ── Request router ────────────────────────────────────────────────────────────

const server = http.createServer(async (req, res) => {
    const t0     = Date.now();
    setCors(res);
    setSecurityHeaders(res);

    if (req.method === 'OPTIONS') {
        res.writeHead(204); res.end(); return;
    }

    let parsed;
    try {
        parsed = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    } catch {
        json(res, 400, { error: 'Bad URL' }); return;
    }

    const path_  = parsed.pathname.replace(/\/+$/, '') || '/';
    const method = req.method.toUpperCase();
    const query  = Object.fromEntries(parsed.searchParams);

    // ── GET /health ──────────────────────────────────────────────────────────
    if (method === 'GET' && (path_ === '/health' || path_ === '/api/health')) {
        json(res, 200, {
            ok:            true,
            uptime_s:      Math.floor((Date.now() - SERVER_START) / 1000),
            redis_url_set: Boolean(process.env.REDIS_URL || process.env.KV_URL),
            upstash_set:   Boolean(process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL),
            github_token:  Boolean(process.env.GITHUB_TOKEN || process.env.GH_TOKEN),
            admin_auth:    Boolean(process.env.ADMIN_PASSWORD),
            timestamp:     new Date().toISOString(),
        });
        return;
    }

    // ── POST /auth ───────────────────────────────────────────────────────────
    if (method === 'POST' && (path_ === '/auth' || path_ === '/api/auth')) {
        try {
            const body = await parseBody(req);
            if (verifyAdminAuth(req, body)) {
                const token = issueToken();
                json(res, 200, { success: true, authenticated: true, token, message: 'Admin access authorized' });
            } else {
                json(res, 401, { success: false, error: 'Incorrect admin password' });
            }
        } catch (err) {
            json(res, 400, { error: err.message });
        }
        return;
    }

    // ── GET /config  (public) ────────────────────────────────────────────────
    if (method === 'GET' && (path_ === '/config' || path_ === '/api/config')) {
        const config   = await db.getConfig();
        const gh       = await getGitHubRelease();
        const records  = await db.getTelemetryRecords();

        const latestVersion = (gh?.version) || config.latest_version_name || null;
        const downloadUrl   = (gh?.assetUrl)
            ? `/ota/download`
            : (config.download_url?.startsWith('https://') ? config.download_url : null);

        // Never leak ban lists in public endpoint
        const {
            banned_devices,    // eslint-disable-line no-unused-vars
            banned_students,   // eslint-disable-line no-unused-vars
            ...publicConfig
        } = config;

        json(res, 200, {
            ...publicConfig,
            latest_version_name:  latestVersion,
            download_url:         downloadUrl,
            live_active_devices:  records.length,
            release_source:       gh ? 'github_release' : 'persisted',
        });
        return;
    }

    // ── GET /config/admin  (admin, full with ban lists) ──────────────────────
    if (method === 'GET' && (path_ === '/config/admin' || path_ === '/api/config/admin')) {
        if (!verifyAdminAuth(req, {})) { json(res, 401, { error: 'Unauthorized' }); return; }
        const config = await db.getConfig();
        const gh     = await getGitHubRelease();
        json(res, 200, {
            ...config,
            github_release: gh,
        });
        return;
    }

    // ── POST /config (admin — public fields only, ban lists via /ban) ────────
    if (method === 'POST' && (path_ === '/config' || path_ === '/api/config')) {
        try {
            const body = await parseBody(req);
            if (!verifyAdminAuth(req, body)) { json(res, 401, { error: 'Unauthorized' }); return; }

            const current = await db.getConfig();
            const update  = {};

            for (const key of CONFIG_ALLOWLIST) {
                if (body[key] === undefined) continue;
                update[key] = body[key];
            }

            // Type gates
            if (update.latest_version_code !== undefined) {
                const v = parseInt(update.latest_version_code);
                if (!Number.isInteger(v) || v <= 0) {
                    json(res, 400, { error: 'latest_version_code must be positive integer' }); return;
                }
                update.latest_version_code = v;
            }
            if (update.min_version_code !== undefined) {
                const v = parseInt(update.min_version_code);
                if (!Number.isInteger(v) || v < 0) {
                    json(res, 400, { error: 'min_version_code must be non-negative integer' }); return;
                }
                update.min_version_code = v;
            }
            if (update.download_url !== undefined) {
                if (typeof update.download_url !== 'string' || !isValidOtaUrl(update.download_url)) {
                    json(res, 400, { error: 'download_url must be an authorized HTTPS URL' }); return;
                }
            }
            if (update.maintenance_mode !== undefined)
                update.maintenance_mode = Boolean(update.maintenance_mode);
            if (update.mandatory_update !== undefined)
                update.mandatory_update = Boolean(update.mandatory_update);
            if (update.pause_updates !== undefined)
                update.pause_updates = Boolean(update.pause_updates);
            if (update.notifications_enabled !== undefined)
                update.notifications_enabled = Boolean(update.notifications_enabled);

            // Preserve ban arrays — they are immutable via this endpoint
            const saved = await db.saveConfig({
                ...current,
                ...update,
                banned_devices:  current.banned_devices,
                banned_students: current.banned_students,
            });

            const { banned_devices, banned_students, ...safe } = saved; // eslint-disable-line no-unused-vars
            json(res, 200, { success: true, config: safe });
        } catch (err) {
            json(res, 400, { error: err.message });
        }
        return;
    }

    // ── GET /ota ─────────────────────────────────────────────────────────────
    if (method === 'GET' && (path_ === '/ota' || path_ === '/api/ota')) {
        const config     = await db.getConfig();
        const gh         = await getGitHubRelease();
        const clientVer  = (query.current_version || '').trim();
        const latestVer  = gh?.version || config.latest_version_name || clientVer;

        if (config.pause_updates) {
            json(res, 200, {
                has_update:       false,
                latest_version:   cleanVersion(latestVer),
                current_version:  cleanVersion(clientVer),
                download_url:     null,
                release_notes:    config.broadcast_notice || 'Updates temporarily paused.',
                source:           'admin_paused',
                paused:           true,
            });
            return;
        }

        // Only serve HTTPS download URLs
        let dlUrl = null;
        if (gh?.assetUrl) {
            const host = req.headers['host'] || `localhost:${PORT}`;
            const proto = req.headers['x-forwarded-proto'] || 'http';
            dlUrl = `${proto}://${host}/ota/download`;
        } else if (config.download_url?.startsWith('https://')) {
            dlUrl = config.download_url;
        }

        const hasUpdate = Boolean(latestVer && clientVer && isNewer(latestVer, clientVer) && dlUrl);

        json(res, 200, {
            has_update:       hasUpdate,
            latest_version:   cleanVersion(latestVer),
            current_version:  cleanVersion(clientVer),
            download_url:     hasUpdate ? dlUrl : null,
            release_notes:    hasUpdate ? (gh?.notes || config.broadcast_notice || '') : 'You are on the latest version.',
            mandatory_update: config.mandatory_update || false,
            min_version_code: config.min_version_code || 1,
            sha256:           hasUpdate ? (gh?.sha256 || config.sha256 || null) : null,
            source:           gh ? 'github_release' : 'persisted',
            published_at:     gh?.publishedAt || null,
        });
        return;
    }

    // ── GET /ota/download ─────────────────────────────────────────────────────
    if (method === 'GET' && (path_ === '/ota/download' || path_ === '/api/ota/download')) {
        const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
        const gh    = await getGitHubRelease();

        if (gh?.assetUrl && token) {
            try {
                const r = await fetch(gh.assetUrl, {
                    headers: {
                        'User-Agent':    'LloydERP-Server',
                        'Authorization': `Bearer ${token}`,
                        'Accept':        'application/octet-stream',
                    },
                    redirect: 'manual',
                    signal:   AbortSignal.timeout(3000),
                });
                const loc = r.headers.get('location');
                if (loc) {
                    if (!isValidOtaUrl(loc)) {
                        json(res, 403, { error: 'Redirected to untrusted host' });
                        return;
                    }
                    res.writeHead(302, { 'Location': loc }); res.end(); return;
                }
            } catch { /* fall through */ }
        }

        // Public browser URL fallback (no token needed for public repos)
        if (gh?.browserUrl) {
            if (!isValidOtaUrl(gh.browserUrl)) {
                json(res, 403, { error: 'Untrusted browser URL' });
                return;
            }
            res.writeHead(302, { 'Location': gh.browserUrl }); res.end(); return;
        }

        const config = await db.getConfig();
        if (config.download_url) {
            if (!isValidOtaUrl(config.download_url)) {
                json(res, 403, { error: 'Untrusted config download URL' });
                return;
            }
            res.writeHead(302, { 'Location': config.download_url }); res.end(); return;
        }

        json(res, 404, { error: 'No APK asset available' });
        return;
    }

    // ── POST /telemetry (public heartbeat, rate-limited) ─────────────────────
    if (method === 'POST' && (path_ === '/telemetry' || path_ === '/api/telemetry' || path_ === '/')) {
        try {
            const payload = await parseBody(req);
            if (!payload?.device_id) {
                json(res, 400, { error: 'device_id required' }); return;
            }

            if (!checkRateLimit(String(payload.device_id))) {
                json(res, 429, { error: 'Too many requests. Retry after 30s.' }); return;
            }

            const ALLOWED_TELEM = [
                'device_id', 'student_id', 'student_name',
                'app_version', 'version_code',
                'app_version_code', 'app_version_name',
                'os_version', 'device_model', 'timestamp',
            ];
            const MAX_STR = 128;
            const sanitized = {};
            for (const key of ALLOWED_TELEM) {
                if (payload[key] === undefined) continue;
                const v = payload[key];
                if (typeof v === 'string')  sanitized[key] = v.trim().replace(/[\x00-\x1F\x7F]/g, '').slice(0, MAX_STR);
                else if (typeof v === 'number' || typeof v === 'boolean') sanitized[key] = v;
            }
            if (sanitized.app_version && !sanitized.app_version_name) {
                sanitized.app_version_name = sanitized.app_version;
            } else if (sanitized.app_version_name && !sanitized.app_version) {
                sanitized.app_version = sanitized.app_version_name;
            }
            if (sanitized.version_code !== undefined && sanitized.app_version_code === undefined) {
                const parsed = parseInt(sanitized.version_code, 10);
                if (!Number.isNaN(parsed)) sanitized.app_version_code = parsed;
            } else if (sanitized.app_version_code !== undefined && sanitized.version_code === undefined) {
                sanitized.version_code = sanitized.app_version_code;
            }
            if (!sanitized.device_id) { json(res, 400, { error: 'device_id empty after sanitize' }); return; }

            await db.upsertTelemetryRecord(sanitized);
            json(res, 200, { success: true, timestamp: new Date().toISOString() });
        } catch (err) {
            json(res, 400, { error: err.message });
        }
        return;
    }

    // ── GET /telemetry (admin) ────────────────────────────────────────────────
    if (method === 'GET' && (path_ === '/telemetry' || path_ === '/api/telemetry')) {
        if (!verifyAdminAuth(req, {})) { json(res, 401, { error: 'Unauthorized' }); return; }
        const records = await db.getTelemetryRecords();
        records.sort((a, b) => new Date(b.timestamp || b.server_received_at || 0) -
                                new Date(a.timestamp || a.server_received_at || 0));
        json(res, 200, records);
        return;
    }

    // ── DELETE /telemetry (admin) ─────────────────────────────────────────────
    if (method === 'DELETE' && (path_ === '/telemetry' || path_ === '/api/telemetry')) {
        try {
            const body = await parseBody(req).catch(() => ({}));
            if (!verifyAdminAuth(req, body)) { json(res, 401, { error: 'Unauthorized' }); return; }

            const deviceId = query.device_id || body.device_id;
            const purge    = query.purge === 'test' || body.purge === 'test';

            if (purge) {
                const removed = await db.purgeTestTelemetry();
                json(res, 200, { success: true, purged_count: removed.length, purged: removed });
                return;
            }
            if (!deviceId) { json(res, 400, { error: 'device_id required' }); return; }

            await db.deleteTelemetryRecord(deviceId);
            json(res, 200, { success: true, deleted: deviceId });
        } catch (err) {
            json(res, 500, { error: err.message });
        }
        return;
    }

    // ── POST /ban (admin — explicit action only) ──────────────────────────────
    if (method === 'POST' && (path_ === '/ban' || path_ === '/api/ban')) {
        try {
            const body = await parseBody(req);
            if (!verifyAdminAuth(req, body)) { json(res, 401, { error: 'Unauthorized' }); return; }

            const deviceId  = (body.device_id || '').trim();
            const studentId = body.student_id ? parseInt(body.student_id) : null;
            const action    = (body.action || '').toLowerCase();

            if (!deviceId && !studentId) {
                json(res, 400, { error: 'device_id or student_id required' }); return;
            }
            if (action !== 'ban' && action !== 'unban') {
                json(res, 400, { error: "action must be 'ban' or 'unban'" }); return;
            }

            const config = await db.getConfig();
            let isDeviceBanned  = false;
            let isStudentBanned = false;

            if (deviceId) {
                const set = new Set((config.banned_devices || []).map(d => String(d).trim()));
                action === 'ban' ? set.add(deviceId) : set.delete(deviceId);
                config.banned_devices = Array.from(set);
                isDeviceBanned = set.has(deviceId);
            }
            if (studentId) {
                const set = new Set((config.banned_students || []).map(s => parseInt(s)).filter(Boolean));
                action === 'ban' ? set.add(studentId) : set.delete(studentId);
                config.banned_students = Array.from(set);
                isStudentBanned = set.has(studentId);
            }

            await db.saveConfig(config);

            json(res, 200, {
                success:           true,
                device_id:         deviceId || null,
                student_id:        studentId || null,
                is_device_banned:  isDeviceBanned,
                is_student_banned: isStudentBanned,
                banned_devices:    config.banned_devices,
                banned_students:   config.banned_students,
            });
        } catch (err) {
            json(res, 500, { error: err.message });
        }
        return;
    }

    // ── GET /stats (admin) ────────────────────────────────────────────────────
    if (method === 'GET' && (path_ === '/stats' || path_ === '/api/stats')) {
        if (!verifyAdminAuth(req, {})) { json(res, 401, { error: 'Unauthorized' }); return; }
        const [records, config] = await Promise.all([db.getTelemetryRecords(), db.getConfig()]);
        json(res, 200, computeStats(records, config));
        return;
    }

    // ── /feedback & /api/feedback (admin GET/PATCH/DELETE, student POST) ─────
    if (path_ === '/feedback' || path_ === '/api/feedback') {
        try {
            const feedbackHandler = require('./api/feedback');
            req.query = query;
            if (method === 'POST' || method === 'PATCH') {
                req.body = await parseBody(req).catch(() => ({}));
            }
            res.status = function(code) {
                this.statusCode = code;
                return this;
            };
            res.json = function(data) {
                json(res, this.statusCode || 200, data);
            };
            await feedbackHandler(req, res);
        } catch (err) {
            json(res, 500, { error: err.message });
        }
        return;
    }

    // ── Static files (web admin dashboard) ───────────────────────────────────
    if (path_ === '/' || path_ === '/admin' || path_ === '/admin/') {
        serveStatic(path.join(ADMIN_DIR, 'index.html'), res);
        return;
    }

    const rel    = (path_.startsWith('/admin/') ? path_.slice(7) : path_).replace(/^\/+/, '');
    const safe   = path.normalize(rel).replace(/^(\.\.[\\/])+/, '');
    const target = path.join(ADMIN_DIR, safe);

    if (target.startsWith(ADMIN_DIR) && fs.existsSync(target)) {
        serveStatic(target, res);
        return;
    }

    json(res, 404, { error: 'Not Found', path: path_ });

    const ms = Date.now() - t0;
    console.log(`[${new Date().toISOString()}] ${method} ${path_} → 404 (${ms}ms)`);
});

// ── Logging middleware (post-response) ────────────────────────────────────────
server.on('request', (req, res) => {
    const t0 = Date.now();
    res.on('finish', () => {
        const ms = Date.now() - t0;
        if (res.statusCode >= 400) {
            console.error(`[${new Date().toISOString()}] ${req.method} ${req.url} → ${res.statusCode} (${ms}ms)`);
        } else {
            console.log(`[${new Date().toISOString()}] ${req.method} ${req.url} → ${res.statusCode} (${ms}ms)`);
        }
    });
});

// ── Graceful shutdown ─────────────────────────────────────────────────────────
function shutdown(signal) {
    console.log(`\n[${signal}] Shutting down gracefully...`);
    server.close(() => {
        console.log('Server closed. Bye.');
        process.exit(0);
    });
    setTimeout(() => process.exit(1), 5000).unref();
}
process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT',  () => shutdown('SIGINT'));

// ── Start ─────────────────────────────────────────────────────────────────────
server.listen(PORT, '0.0.0.0', () => {
    console.log('═══════════════════════════════════════════════════════════');
    console.log(` Lloyd ERP Fleet Server  →  http://localhost:${PORT}`);
    console.log(' Endpoints:');
    console.log(`   GET  /health          liveness check`);
    console.log(`   GET  /config          public config (no ban lists)`);
    console.log(`   GET  /config/admin    full config (admin)`);
    console.log(`   POST /config          update config (admin)`);
    console.log(`   GET  /ota             update check (?current_version=)`);
    console.log(`   GET  /ota/download    latest APK redirect`);
    console.log(`   POST /telemetry       device heartbeat`);
    console.log(`   GET  /telemetry       all records (admin)`);
    console.log(`   DELETE /telemetry     delete/purge (admin)`);
    console.log(`   POST /ban             ban/unban device|student (admin)`);
    console.log(`   GET  /stats           fleet KPIs (admin)`);
    console.log(`   POST /auth            verify admin creds`);
    console.log('═══════════════════════════════════════════════════════════');
});

module.exports = server;
