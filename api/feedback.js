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

const crypto = require('crypto');
const db = require('./lib/db');
const { verifyAdminAuth } = require('./lib/auth');

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-admin-key, x-admin-password, Accept');
    res.setHeader('Cache-Control', 'no-store');
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

            const list = await db.getFeedbackRecords();
            const nextList = [entry, ...list];
            const saveResult = await db.saveFeedback(nextList);
            if (!saveResult || !saveResult.ok) {
                res.status(503).json({ error: saveResult?.error || 'Failed to persist feedback' });
                return;
            }

            res.status(201).json({ success: true, id: entry.id, submitted_at: entry.submitted_at });
            return;
        }

        // ── GET: Admin lists feedback ─────────────────────────────────────────
        if (req.method === 'GET') {
            if (!verifyAdminAuth(req)) {
                res.status(401).json({ error: 'Unauthorized: Admin access required' });
                return;
            }

            const list = await db.getFeedbackRecords();
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

            const list = await db.getFeedbackRecords();
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

            const nextList = [...list];
            nextList[idx] = { ...nextList[idx], ...patch };
            const saveResult = await db.saveFeedback(nextList);
            if (!saveResult || !saveResult.ok) {
                res.status(503).json({ error: saveResult?.error || 'Failed to persist feedback update' });
                return;
            }

            res.status(200).json({ success: true, feedback: nextList[idx] });
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

            const list = await db.getFeedbackRecords();
            const idx = list.findIndex(f => f.id === id);
            if (idx === -1) {
                res.status(404).json({ error: 'Feedback entry not found' });
                return;
            }

            const nextList = [...list];
            const [deleted] = nextList.splice(idx, 1);
            const saveResult = await db.saveFeedback(nextList);
            if (!saveResult || !saveResult.ok) {
                res.status(503).json({ error: saveResult?.error || 'Failed to persist feedback deletion' });
                return;
            }

            res.status(200).json({ success: true, deleted: deleted.id });
            return;
        }

        res.status(405).json({ error: 'Method not allowed' });
    } catch (err) {
        console.error('[API/FEEDBACK] Handler error:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};
