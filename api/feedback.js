/**
 * Lloyd ERP — Student Feedback Fleet API (Vercel Serverless)
 *
 * POST   /api/feedback          — Student submits feedback (no auth)
 * GET    /api/feedback          — Admin lists all feedback (admin auth required)
 * PATCH  /api/feedback?id=:id   — Admin updates status/notes (admin auth required)
 * DELETE /api/feedback?id=:id   — Admin removes an entry (admin auth required)
 *
 * Storage: Upstash REST → Redis TCP → disk (feedback_store.json, gitignored)
 * Zero dependency on Lloyd ERP API. All data stays on fleet server.
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { verifyAdminAuth } = require('./lib/auth');

const FEEDBACK_DISK_PATH = path.join(process.cwd(), 'feedback_store.json');
const REDIS_KEY = 'lloyd_fleet_feedback';

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-admin-key, x-admin-password, Accept');
    res.setHeader('Cache-Control', 'no-store');
}

// ─── Upstash REST helper (mirrors api/lib/db.js pattern) ─────────────────────

function getUpstashConfig() {
    const url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
    const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
    if (url && token) return { url: url.replace(/\/+$/, ''), token };
    return null;
}

async function upstashCmd(command, ...args) {
    const conf = getUpstashConfig();
    if (!conf) return null;
    try {
        const resp = await fetch(conf.url, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${conf.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify([command, ...args]),
            signal: AbortSignal.timeout(1200)
        });
        if (!resp.ok) return null;
        const data = await resp.json();
        return data.result;
    } catch (_) {
        return null;
    }
}

// ─── Disk helpers ─────────────────────────────────────────────────────────────

function readDisk() {
    try {
        if (fs.existsSync(FEEDBACK_DISK_PATH)) {
            const raw = fs.readFileSync(FEEDBACK_DISK_PATH, 'utf8');
            const data = JSON.parse(raw);
            if (Array.isArray(data)) return data;
        }
    } catch (_) {}
    return [];
}

function writeDisk(list) {
    try {
        fs.writeFileSync(FEEDBACK_DISK_PATH, JSON.stringify(list, null, 2), 'utf8');
    } catch (_) {}
}

// ─── In-memory cache ──────────────────────────────────────────────────────────

let mem = null; // null = not loaded yet

function getMemFeedback() {
    if (mem === null) mem = readDisk();
    return mem;
}

// ─── Persistence write (all tiers) ───────────────────────────────────────────

async function persistAll(list) {
    mem = list;
    writeDisk(list);
    try { await upstashCmd('set', REDIS_KEY, JSON.stringify(list)); } catch (_) {}
}

// ─── Load & merge from all tiers ─────────────────────────────────────────────

async function loadAll() {
    // Start from disk/memory
    const byId = {};
    for (const item of getMemFeedback()) {
        if (item && item.id) byId[item.id] = item;
    }

    // Merge from Upstash (fresher on Vercel deployments)
    try {
        const raw = await upstashCmd('get', REDIS_KEY);
        if (raw) {
            const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw;
            if (Array.isArray(parsed)) {
                for (const item of parsed) {
                    if (item && item.id) {
                        const existing = byId[item.id];
                        const itemDate = new Date(item.submitted_at || 0);
                        const existDate = existing ? new Date(existing.submitted_at || 0) : 0;
                        if (!existing || itemDate >= existDate) byId[item.id] = item;
                    }
                }
            }
        }
    } catch (_) {}

    const list = Object.values(byId);
    list.sort((a, b) => new Date(b.submitted_at) - new Date(a.submitted_at));
    mem = list;
    return list;
}

// ─── Input validation ─────────────────────────────────────────────────────────

const ALLOWED_CATEGORIES = ['general', 'bug', 'feature', 'attendance', 'marks', 'ui', 'other'];
const ALLOWED_STATUSES   = ['new', 'read', 'actioned', 'closed'];
const MAX_MESSAGE_LEN    = 2000;

function sanitize(str, maxLen = 500) {
    if (typeof str !== 'string') return '';
    return str.trim().slice(0, maxLen);
}

// ─── Route handler ────────────────────────────────────────────────────────────

module.exports = async function handler(req, res) {
    setCors(res);

    if (req.method === 'OPTIONS') {
        res.status(204).end();
        return;
    }

    try {
        // ── POST: Student submits feedback ────────────────────────────────────
        if (req.method === 'POST') {
            let body = req.body;
            if (typeof body === 'string') { try { body = JSON.parse(body); } catch (_) {} }
            body = body || {};

            const message = sanitize(body.message, MAX_MESSAGE_LEN);
            if (!message) {
                res.status(400).json({ error: 'message is required and must be non-empty' });
                return;
            }

            const entry = {
                id:            crypto.randomBytes(12).toString('hex'),
                submitted_at:  new Date().toISOString(),
                category:      ALLOWED_CATEGORIES.includes(body.category) ? body.category : 'general',
                message,
                student_id:    body.student_id  ? sanitize(String(body.student_id), 20)  : null,
                app_version:   body.app_version ? sanitize(String(body.app_version), 20) : null,
                device_model:  body.device_model ? sanitize(String(body.device_model), 50) : null,
                status:        'new',
                admin_notes:   null,
                resolved_at:   null
            };

            const list = await loadAll();
            list.unshift(entry);
            await persistAll(list);

            res.status(201).json({ success: true, id: entry.id, submitted_at: entry.submitted_at });
            return;
        }

        // ── GET: Admin lists feedback ─────────────────────────────────────────
        if (req.method === 'GET') {
            if (!verifyAdminAuth(req)) {
                res.status(401).json({ error: 'Unauthorized: Admin access required' });
                return;
            }

            const list = await loadAll();
            const { status, category, limit, offset } = req.query || {};

            let filtered = list;
            if (status)   filtered = filtered.filter(f => f.status   === status);
            if (category) filtered = filtered.filter(f => f.category === category);

            const off = Math.max(0, parseInt(offset) || 0);
            const lim = Math.min(200, Math.max(1, parseInt(limit) || 50));

            res.status(200).json({
                total:    filtered.length,
                offset:   off,
                limit:    lim,
                feedback: filtered.slice(off, off + lim)
            });
            return;
        }

        // ── PATCH: Admin updates a feedback entry ─────────────────────────────
        if (req.method === 'PATCH') {
            if (!verifyAdminAuth(req)) {
                res.status(401).json({ error: 'Unauthorized: Admin access required' });
                return;
            }

            const id = req.query && req.query.id;
            if (!id) {
                res.status(400).json({ error: 'Query param id is required' });
                return;
            }

            let body = req.body;
            if (typeof body === 'string') { try { body = JSON.parse(body); } catch (_) {} }
            body = body || {};

            const list = await loadAll();
            const idx = list.findIndex(f => f.id === id);
            if (idx === -1) {
                res.status(404).json({ error: 'Feedback entry not found' });
                return;
            }

            const patch = {};
            if (body.status && ALLOWED_STATUSES.includes(body.status)) {
                patch.status = body.status;
                if (body.status === 'closed' || body.status === 'actioned') {
                    patch.resolved_at = new Date().toISOString();
                }
            }
            if (body.admin_notes !== undefined) {
                patch.admin_notes = sanitize(String(body.admin_notes || ''), MAX_MESSAGE_LEN);
            }

            list[idx] = { ...list[idx], ...patch };
            await persistAll(list);

            res.status(200).json({ success: true, feedback: list[idx] });
            return;
        }

        // ── DELETE: Admin removes a feedback entry ────────────────────────────
        if (req.method === 'DELETE') {
            if (!verifyAdminAuth(req)) {
                res.status(401).json({ error: 'Unauthorized: Admin access required' });
                return;
            }

            const id = req.query && req.query.id;
            if (!id) {
                res.status(400).json({ error: 'Query param id is required' });
                return;
            }

            const list = await loadAll();
            const idx = list.findIndex(f => f.id === id);
            if (idx === -1) {
                res.status(404).json({ error: 'Feedback entry not found' });
                return;
            }

            const [deleted] = list.splice(idx, 1);
            await persistAll(list);

            res.status(200).json({ success: true, deleted: deleted.id });
            return;
        }

        res.status(405).json({ error: 'Method not allowed' });
    } catch (err) {
        console.error('[API/FEEDBACK] Handler error:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};
