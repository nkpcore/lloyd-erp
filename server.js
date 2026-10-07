/**
 * Lloyd ERP — Zero-Dependency Fleet Telemetry & Admin Governance Server
 * Pure Node.js Standard Library (http, fs, path, url)
 *
 * Capabilities:
 * 1. Serves Web Admin Dashboard (web/admin/) at / and /admin/
 * 2. GET /config: Serves dynamic fleet governance configuration (min versions, bans, maintenance)
 * 3. POST /config: Atomically saves updated fleet config to fleet_config.json
 * 4. GET /telemetry: Returns live fleet heartbeat records
 * 5. POST /telemetry: Ingests client heartbeats from Android devices into fleet_telemetry.json
 * 6. POST /ban: 1-click UUID ban/unban endpoint
 * 7. Full CORS support for multi-device local network and cloud access
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
    min_version_code: 15,
    latest_version_name: 'v1.0.15',
    download_url: 'https://github.com/nkpcore/lloyd-erp/releases/latest',
    banned_students: [],
    banned_devices: [],
    broadcast_notice: null,
    maintenance_mode: false,
    maintenance_message: 'Lloyd ERP service is currently undergoing routine maintenance.'
};

// Initialize configuration file if absent
function getFleetConfig() {
    try {
        if (fs.existsSync(CONFIG_FILE)) {
            const raw = fs.readFileSync(CONFIG_FILE, 'utf8');
            return JSON.parse(raw);
        }
    } catch (err) {
        console.error('[CONFIG] Error reading config file, falling back to default:', err.message);
    }
    saveFleetConfig(DEFAULT_CONFIG);
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
            if (body.length > 1e6) { // 1MB limit
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

    // --- API ROUTES ---

    // GET /config or GET /api/config
    if (req.method === 'GET' && (pathname === '/config' || pathname === '/api/config')) {
        const config = getFleetConfig();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(config, null, 2));
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
                min_version_code: parseInt(body.min_version_code) || current.min_version_code,
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
                console.log(`[TELEMETRY] Heartbeat received from device: ${payload.device_id} (Student ID: ${payload.student_id})`);
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
            const set = new Set(config.banned_devices || []);
            if (body.banned === false) {
                set.delete(deviceId);
            } else {
                set.add(deviceId);
            }
            config.banned_devices = Array.from(set);
            saveFleetConfig(config);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: true, banned_devices: config.banned_devices }));
        } catch (err) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: err.message }));
        }
        return;
    }

    // --- STATIC FILES FOR WEB ADMIN ---
    if (req.method === 'GET') {
        let relativePath = parsedUrl.pathname;
        if (relativePath === '/' || relativePath === '/admin' || relativePath === '/admin/') {
            relativePath = '/index.html';
        } else if (relativePath.startsWith('/admin/')) {
            relativePath = relativePath.substring('/admin'.length);
        }

        const safePath = path.normalize(relativePath).replace(/^(\.\.[\/\\])+/, '');
        const targetPath = path.join(ADMIN_DIR, safePath);

        // Security check: ensure path stays within ADMIN_DIR
        if (!targetPath.startsWith(ADMIN_DIR)) {
            res.writeHead(403, { 'Content-Type': 'text/plain' });
            res.end('403 Forbidden');
            return;
        }

        serveStaticFile(targetPath, res);
        return;
    }

    // Fallback
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('404 Not Found');
});

// Start server
server.listen(PORT, '0.0.0.0', () => {
    console.log(`=======================================================`);
    console.log(` Lloyd ERP Fleet Telemetry & Governance Server Started `);
    console.log(` Web Admin Dashboard: http://localhost:${PORT}/        `);
    console.log(` Config Endpoint:     http://localhost:${PORT}/config `);
    console.log(` Telemetry Endpoint:  http://localhost:${PORT}/telemetry `);
    console.log(`=======================================================`);
});
