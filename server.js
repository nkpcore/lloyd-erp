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

// Read configuration file
function getFleetConfig() {
    try {
        if (fs.existsSync(CONFIG_FILE)) {
            const raw = fs.readFileSync(CONFIG_FILE, 'utf8');
            return JSON.parse(raw);
        }
    } catch (err) {
        console.error('[CONFIG] Error reading config file, falling back to default:', err.message);
    }
    return DEFAULT_CONFIG;
}

function saveFleetConfig(config) {
    try {
        fs.writeFileSync(CONFIG_FILE, JSON.stringify(config, null, 2), 'utf8');
        return true;
    } catch (err) {
        console.error('[CONFIG] Error writing config file:', err.message);
        return false;
    }
}

// In-memory / JSON persistence for telemetry records
function getTelemetryRecords() {
    try {
        if (fs.existsSync(TELEMETRY_FILE)) {
            const raw = fs.readFileSync(TELEMETRY_FILE, 'utf8');
            const data = JSON.parse(raw);
            return Array.isArray(data) ? data : Object.values(data);
        }
    } catch (err) {
        console.error('[TELEMETRY] Error reading telemetry file:', err.message);
    }
    return [];
}

function getLiveFleetVersion(records) {
    if (!Array.isArray(records) || records.length === 0) return null;
    const sorted = records
        .map(r => ({ name: (r.app_version || '').trim(), code: parseInt(r.version_code) || 0 }))
        .filter(v => v.name || v.code > 0)
        .sort((a, b) => b.code - a.code);
    return sorted.length > 0 ? sorted[0].name : null;
}

function upsertTelemetryRecord(record) {
    if (!record || !record.device_id) return false;
    try {
        let store = {};
        if (fs.existsSync(TELEMETRY_FILE)) {
            try {
                const raw = fs.readFileSync(TELEMETRY_FILE, 'utf8');
                const parsed = JSON.parse(raw);
                if (Array.isArray(parsed)) {
                    parsed.forEach(r => { if (r.device_id) store[r.device_id] = r; });
                } else if (typeof parsed === 'object') {
                    store = parsed;
                }
            } catch (e) {}
        }
        store[record.device_id] = {
            ...record,
            server_received_at: new Date().toISOString()
        };
        fs.writeFileSync(TELEMETRY_FILE, JSON.stringify(Object.values(store), null, 2), 'utf8');
        return true;
    } catch (err) {
        console.error('[TELEMETRY] Error upserting record:', err.message);
        return false;
    }
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
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept, X-Requested-With');
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

const server = http.createServer(async (req, res) => {
    setCorsHeaders(res);

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    const parsedUrl = new URL(req.url, 'http://' + (req.headers.host || 'localhost'));
    const pathname = parsedUrl.pathname.replace(/\/+$/, '') || '/';

    console.log(`[${new Date().toISOString()}] ${req.method} ${pathname}`);

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

        const resp = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, { headers });
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

    // --- API ROUTES ---

    // GET /config or GET /api/config
    if (req.method === 'GET' && (pathname === '/config' || pathname === '/api/config')) {
        const config = getFleetConfig();
        const records = getTelemetryRecords();
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
            const current = getFleetConfig();
            const updated = {
                ...current,
                ...body,
                min_version_code: body.min_version_code !== undefined ? parseInt(body.min_version_code) : current.min_version_code,
                latest_version_name: body.latest_version_name !== undefined ? (body.latest_version_name || null) : current.latest_version_name,
                download_url: body.download_url !== undefined ? body.download_url : current.download_url,
                banned_devices: Array.isArray(body.banned_devices) ? [...new Set(body.banned_devices)] : current.banned_devices,
                banned_students: Array.isArray(body.banned_students) ? [...new Set(body.banned_students)] : current.banned_students,
                maintenance_mode: typeof body.maintenance_mode === 'boolean' ? body.maintenance_mode : current.maintenance_mode
            };
            saveFleetConfig(updated);
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
        const config = getFleetConfig();
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
        const config = getFleetConfig();
        const records = getTelemetryRecords();
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
        const records = getTelemetryRecords();
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
                upsertTelemetryRecord(payload);
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

    // POST /ban (Quick toggle ban for device UUID)
    if (req.method === 'POST' && pathname === '/ban') {
        try {
            const body = await parseJsonBody(req);
            const deviceId = (body.device_id || '').trim();
            if (!deviceId) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'device_id required' }));
                return;
            }

            const config = getFleetConfig();
            const bans = new Set(config.banned_devices || []);
            let isBanned = false;

            if (bans.has(deviceId)) {
                bans.delete(deviceId);
                isBanned = false;
            } else {
                bans.add(deviceId);
                isBanned = true;
            }

            config.banned_devices = Array.from(bans);
            saveFleetConfig(config);

            console.log(`[BAN] Device ${deviceId} ban toggled: ${isBanned}`);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: true, device_id: deviceId, is_banned: isBanned, total_banned: config.banned_devices.length }));
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

    const safePath = path.normalize(pathname).replace(/^(\.\.[\/\\])+/, '');
    const targetPath = path.join(ADMIN_DIR, safePath);

    if (targetPath.startsWith(ADMIN_DIR)) {
        serveStaticFile(targetPath, res);
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
