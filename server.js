/**
 * Lloyd ERP — Zero-Dependency Fleet Telemetry & Admin Governance Server
 * Pure Node.js Standard Library (http, fs, path, url)
 * ZERO HARDCODED VERSIONS: Computes latest release and version adoption dynamically from live telemetry.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = process.env.PORT || 8080;
const ROOT_DIR = __dirname;
const ADMIN_DIR = path.join(ROOT_DIR, 'web', 'admin');
const CONFIG_FILE = path.join(ROOT_DIR, 'fleet_config.json');
const TELEMETRY_FILE = path.join(ROOT_DIR, 'fleet_telemetry.json');

const DEFAULT_CONFIG = {
    min_version_code: 1,
    latest_version_name: null,
    download_url: '',
    banned_students: [],
    banned_devices: [],
    broadcast_notice: null,
    maintenance_mode: false,
    maintenance_message: 'Lloyd ERP service is currently undergoing routine maintenance.'
};

function cleanVersion(v) {
    return (v || '').trim().replace(/^[vV]/, '').trim();
}

function isNewerVersion(remote, current) {
    const rParts = cleanVersion(remote).split('.').map(n => parseInt(n) || 0);
    const cParts = cleanVersion(current).split('.').map(n => parseInt(n) || 0);
    const maxLen = Math.max(rParts.length, cParts.length);
    for (let i = 0; i < maxLen; i++) {
        const r = rParts[i] || 0;
        const c = cParts[i] || 0;
        if (r > c) return true;
        if (r < c) return false;
    }
    return false;
}

const db = require('./api/lib/db');
const { verifyAdminAuth } = require('./api/lib/auth');

// Read configuration from unified persistence layer
async function getFleetConfig() {
    return await db.getConfig();
}

async function saveFleetConfig(config) {
    return await db.saveConfig(config);
}

// Telemetry records from unified persistence layer
async function getTelemetryRecords() {
    return await db.getTelemetryRecords();
}

function getLiveFleetVersion(records) {
    if (!Array.isArray(records) || records.length === 0) return null;
    const sorted = records
        .map(r => ({ name: (r.app_version || '').trim(), code: parseInt(r.version_code) || 0 }))
        .filter(v => v.name || v.code > 0)
        .sort((a, b) => b.code - a.code);
    return sorted.length > 0 ? sorted[0].name : null;
}

async function upsertTelemetryRecord(record) {
    return await db.upsertTelemetryRecord(record);
}

const MIME_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.ico': 'image/x-icon',
    '.txt': 'text/plain; charset=utf-8'
};

function setCorsHeaders(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept, X-Requested-With, x-admin-key, x-admin-password');
}

function parseJsonBody(req) {
    return new Promise((resolve, reject) => {
        let body = '';
        req.on('data', chunk => {
            body += chunk.toString();
            if (body.length > 1e6) {
                req.destroy();
                reject(new Error('Payload too large'));
            }
        });
        req.on('end', () => {
            if (!body.trim()) return resolve({});
            try {
                resolve(JSON.parse(body));
            } catch (e) {
                reject(new Error('Invalid JSON'));
            }
        });
        req.on('error', reject);
    });
}

function serveStaticFile(filePath, res) {
    fs.stat(filePath, (err, stats) => {
        if (err || !stats.isFile()) {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('404 Not Found');
            return;
        }

        const ext = path.extname(filePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';

        res.writeHead(200, {
            'Content-Type': contentType,
            'Content-Length': stats.size,
            'Cache-Control': 'no-cache'
        });

        const readStream = fs.createReadStream(filePath);
        readStream.pipe(res);
    });
}

let cachedGhRelease = null;
let lastGhCheckTime = 0;

async function getLiveGitHubRelease() {
    const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
    const repo = process.env.GITHUB_REPOSITORY || 'nkpcore/lloyd-erp';
    const now = Date.now();

    if (cachedGhRelease && (now - lastGhCheckTime < 60000)) {
        return cachedGhRelease;
    }

    try {
        const headers = {
            'User-Agent': 'LloydERP-Local-Server',
            'Accept': 'application/vnd.github.v3+json'
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const resp = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, {
            headers,
            signal: AbortSignal.timeout(2000)
        });
        if (!resp.ok) return null;

        const release = await resp.json();
        const apkAsset = (release.assets || []).find(a => (a.name || '').toLowerCase().endsWith('.apk'));

        cachedGhRelease = {
            version: cleanVersion(release.tag_name || release.name || ''),
            notes: release.body || release.name || 'New release available from GitHub.',
            assetId: apkAsset ? apkAsset.id : null,
            assetUrl: apkAsset ? apkAsset.url : null,
            browserDownloadUrl: apkAsset ? apkAsset.browser_download_url : null
        };
        lastGhCheckTime = now;
        return cachedGhRelease;
    } catch (e) {
        return null;
    }
}

const server = http.createServer(async (req, res) => {
    setCorsHeaders(res);

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    const parsedUrl = new URL(req.url, 'http://' + (req.headers.host || 'localhost'));
    const pathname = parsedUrl.pathname.replace(/\/+$/, '') || '/';
    req.query = Object.fromEntries(parsedUrl.searchParams);

    console.log(`[${new Date().toISOString()}] ${req.method} ${pathname}`);

    // --- API ROUTES ---

    // POST /auth or POST /api/auth (Admin Password Verification Gate)
    if (req.method === 'POST' && (pathname === '/auth' || pathname === '/api/auth')) {
        try {
            const body = await parseJsonBody(req);
            if (verifyAdminAuth(req, body)) {
                const token = (process.env.ADMIN_PASSWORD || 'loyderp').trim();
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({
                    success: true,
                    authenticated: true,
                    token: token,
                    message: 'Admin access authorized'
                }));
                return;
            }

            res.writeHead(401, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
                success: false,
                authenticated: false,
                error: 'Incorrect admin password'
            }));
            return;
        } catch (err) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: err.message }));
            return;
        }
    }

    // GET /config or GET /api/config
    if (req.method === 'GET' && (pathname === '/config' || pathname === '/api/config')) {
        const config = await getFleetConfig();
        const records = await getTelemetryRecords();
        const ghRelease = await getLiveGitHubRelease();
        const dynamicLatest = (ghRelease && ghRelease.version) || config.latest_version_name || getLiveFleetVersion(records);
        const downloadUrl = (ghRelease && ghRelease.assetUrl) ? `/ota/download` : (config.download_url || '');

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            ...config,
            latest_version_name: dynamicLatest,
            download_url: downloadUrl,
            live_active_devices: records.length,
            release_source: ghRelease ? 'github_release' : 'fleet_telemetry'
        }, null, 2));
        return;
    }

    // POST /config
    if (req.method === 'POST' && (pathname === '/config' || pathname === '/api/config')) {
        try {
            const body = await parseJsonBody(req);
            if (!verifyAdminAuth(req, body)) {
                res.writeHead(401, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Unauthorized: Invalid admin password' }));
                return;
            }

            const current = await getFleetConfig();
            const updated = {
                ...current,
                ...body,
                min_version_code: body.min_version_code !== undefined ? parseInt(body.min_version_code) : current.min_version_code,
                latest_version_name: body.latest_version_name !== undefined ? (body.latest_version_name || null) : current.latest_version_name,
                download_url: body.download_url !== undefined ? body.download_url : current.download_url,
                banned_devices: Array.isArray(body.banned_devices) ? [...new Set(body.banned_devices)] : current.banned_devices,
                banned_students: Array.isArray(body.banned_students) ? [...new Set(body.banned_students.map(s => parseInt(s)).filter(Boolean))] : current.banned_students,
                maintenance_mode: typeof body.maintenance_mode === 'boolean' ? body.maintenance_mode : current.maintenance_mode,
                maintenance_message: body.maintenance_message !== undefined ? body.maintenance_message : current.maintenance_message,
                broadcast_notice: body.broadcast_notice !== undefined ? body.broadcast_notice : current.broadcast_notice
            };
            await saveFleetConfig(updated);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: true, config: updated }));
        } catch (err) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: err.message }));
        }
        return;
    }

    // GET /ota/download (Direct zero-auth APK proxy from GitHub Release asset or configured URL)
    if (req.method === 'GET' && (pathname === '/ota/download' || pathname === '/api/ota/download' || parsedUrl.searchParams.get('download') === 'latest' || parsedUrl.searchParams.get('download') === 'true')) {
        const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
        const ghRelease = await getLiveGitHubRelease();
        if (ghRelease && ghRelease.assetUrl && token) {
            try {
                const assetResp = await fetch(ghRelease.assetUrl, {
                    headers: {
                        'User-Agent': 'LloydERP-Local-Server',
                        'Authorization': `Bearer ${token}`,
                        'Accept': 'application/octet-stream'
                    },
                    redirect: 'manual'
                });
                const loc = assetResp.headers.get('location');
                if (loc) {
                    res.writeHead(302, { 'Location': loc });
                    res.end();
                    return;
                }
            } catch (e) {}
        }
        const config = await getFleetConfig();
        if (config.download_url) {
            res.writeHead(302, { 'Location': config.download_url });
            res.end();
            return;
        }
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Release APK asset not available yet');
        return;
    }

    // GET /ota or GET /api/ota (Dynamic Live In-App OTA Update Check)
    if (req.method === 'GET' && (pathname === '/ota' || pathname === '/api/ota')) {
        const config = await getFleetConfig();
        const records = await getTelemetryRecords();
        const ghRelease = await getLiveGitHubRelease();
        const clientVer = (parsedUrl.searchParams.get('current_version') || '').trim();
        const latestVer = (ghRelease && ghRelease.version) || config.latest_version_name || getLiveFleetVersion(records) || clientVer;

        let dlUrl = config.download_url || '';
        let notes = config.broadcast_notice || 'New update available from Lloyd Fleet Portal.';

        if (ghRelease && ghRelease.assetUrl) {
            const host = req.headers['host'] || 'localhost:8080';
            dlUrl = `http://${host}/ota/download`;
            notes = ghRelease.notes;
        }

        const hasUpdate = Boolean(latestVer && clientVer && isNewerVersion(latestVer, clientVer) && dlUrl);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            has_update: hasUpdate,
            latest_version: cleanVersion(latestVer),
            current_version: cleanVersion(clientVer),
            download_url: hasUpdate ? dlUrl : null,
            release_notes: hasUpdate ? notes : 'You are running the latest version.',
            source: ghRelease ? 'github_release' : 'fleet_telemetry'
        }, null, 2));
        return;
    }

    // GET /telemetry
    if (req.method === 'GET' && (pathname === '/telemetry' || pathname === '/api/telemetry')) {
        const records = await getTelemetryRecords();
        records.sort((a, b) => new Date(b.timestamp || 0) - new Date(a.timestamp || 0));
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(records, null, 2));
        return;
    }

    // POST /telemetry (Heartbeat from client)
    if (req.method === 'POST' && (pathname === '/telemetry' || pathname === '/api/telemetry' || pathname === '/')) {
        try {
            const payload = await parseJsonBody(req);
            if (payload && payload.device_id) {
                await upsertTelemetryRecord(payload);
                console.log(`[TELEMETRY] Live device heartbeat received: ${payload.device_id} (Student ID: ${payload.student_id})`);
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true, timestamp: new Date().toISOString() }));
                return;
            } else {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Missing device_id in telemetry payload' }));
                return;
            }
        } catch (err) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: err.message }));
            return;
        }
    }

    // DELETE /telemetry
    if (req.method === 'DELETE' && (pathname === '/telemetry' || pathname === '/api/telemetry')) {
        try {
            const body = await parseJsonBody(req).catch(() => ({}));
            if (!verifyAdminAuth(req, body)) {
                res.writeHead(401, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Unauthorized: Invalid admin password' }));
                return;
            }

            const queryDeviceId = parsedUrl.searchParams.get('device_id');
            const queryPurge = parsedUrl.searchParams.get('purge');
            const deviceId = queryDeviceId || body.device_id;
            const purge = queryPurge === 'test' || body.purge === 'test';

            if (purge) {
                const purged = await db.purgeTestTelemetry();
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true, purged_count: purged.length, purged }));
                return;
            }

            if (!deviceId) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'device_id is required' }));
                return;
            }

            await db.deleteTelemetryRecord(deviceId);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: true, deleted: deviceId }));
            return;
        } catch (err) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: err.message }));
            return;
        }
    }

    // POST /ban or POST /api/ban (1-Click Device & Student Account Revocation)
    if (req.method === 'POST' && (pathname === '/ban' || pathname === '/api/ban')) {
        try {
            const body = await parseJsonBody(req);
            if (!verifyAdminAuth(req, body)) {
                res.writeHead(401, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Unauthorized: Invalid admin password' }));
                return;
            }

            const deviceId = (body.device_id || '').trim();
            const studentId = body.student_id ? parseInt(body.student_id) : null;

            if (!deviceId && !studentId) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'device_id or student_id required' }));
                return;
            }

            const config = await getFleetConfig();
            let isDeviceBanned = false;
            let isStudentBanned = false;
            const action = (body.action || 'toggle').toLowerCase();

            if (deviceId) {
                const bans = new Set((config.banned_devices || []).map(d => String(d).trim()));
                if (action === 'ban') {
                    bans.add(deviceId);
                    isDeviceBanned = true;
                } else if (action === 'unban') {
                    bans.delete(deviceId);
                    isDeviceBanned = false;
                } else {
                    if (bans.has(deviceId)) {
                        bans.delete(deviceId);
                        isDeviceBanned = false;
                    } else {
                        bans.add(deviceId);
                        isDeviceBanned = true;
                    }
                }
                config.banned_devices = Array.from(bans);
            }

            if (studentId) {
                const studentBans = new Set((config.banned_students || []).map(s => parseInt(s)).filter(Boolean));
                if (action === 'ban') {
                    studentBans.add(studentId);
                    isStudentBanned = true;
                } else if (action === 'unban') {
                    studentBans.delete(studentId);
                    isStudentBanned = false;
                } else {
                    if (studentBans.has(studentId)) {
                        studentBans.delete(studentId);
                        isStudentBanned = false;
                    } else {
                        studentBans.add(studentId);
                        isStudentBanned = true;
                    }
                }
                config.banned_students = Array.from(studentBans);
            }

            await saveFleetConfig(config);
            console.log(`[BAN] Action ${action} executed — device: ${deviceId} (${isDeviceBanned}), student: ${studentId} (${isStudentBanned})`);

            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
                success: true,
                device_id: deviceId || null,
                student_id: studentId || null,
                is_device_banned: isDeviceBanned,
                is_student_banned: isStudentBanned,
                banned_devices: config.banned_devices || [],
                banned_students: config.banned_students || []
            }));
            return;
        } catch (err) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: err.message }));
            return;
        }
    }

    // --- STATIC FILES (Web Admin) ---
    if (pathname === '/' || pathname === '/admin' || pathname === '/admin/') {
        serveStaticFile(path.join(ADMIN_DIR, 'index.html'), res);
        return;
    }

    const relPath = pathname.startsWith('/admin/') ? pathname.substring(7) : pathname.replace(/^\/+/, '');
    const safePath = path.normalize(relPath).replace(/^(\.\.[\/\\])+/, '');
    const targetPath = path.join(ADMIN_DIR, safePath);

    if (fs.existsSync(targetPath) && targetPath.startsWith(ADMIN_DIR)) {
        serveStaticFile(targetPath, res);
        return;
    }

    const publicTarget = path.join(__dirname, 'public', safePath);
    if (fs.existsSync(publicTarget) && publicTarget.startsWith(__dirname)) {
        serveStaticFile(publicTarget, res);
        return;
    }

    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('404 Not Found');
});

// Restart or start server
server.listen(PORT, '0.0.0.0', () => {
    console.log(`=======================================================`);
    console.log(` Lloyd ERP Fleet Telemetry & Governance Server Started `);
    console.log(` Web Admin Dashboard: http://localhost:${PORT}/        `);
    console.log(` Config Endpoint:     http://localhost:${PORT}/config `);
    console.log(` OTA Check Endpoint:  http://localhost:${PORT}/ota    `);
    console.log(` Telemetry Endpoint:  http://localhost:${PORT}/telemetry `);
    console.log(`=======================================================`);
});

module.exports = server;

