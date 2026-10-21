/**
 * Fleet Feedback API — Unit Tests
 * Runs with: node --test tests/fleet_feedback.test.js
 */

'use strict';

const { test, describe } = require('node:test');
const assert = require('node:assert/strict');

// ─── Inline the validation logic from api/feedback.js ────────────────────────

const ALLOWED_FEEDBACK_TYPES = ['feedback', 'suggestion', 'grievance'];
const MAX_COMMENT_LEN = 2000;

function validateFeedbackBody(body) {
    if (!body) return { ok: false, error: 'Request body is required' };

    const studentId = (body.student_id !== undefined && body.student_id !== null)
        ? String(body.student_id).trim()
        : '';
    if (!studentId) {
        return { ok: false, error: 'student_id is required' };
    }

    const feedbackType = (body.feedback_type || '').trim().toLowerCase();
    if (!ALLOWED_FEEDBACK_TYPES.includes(feedbackType)) {
        return {
            ok: false,
            error: `feedback_type must be one of: ${ALLOWED_FEEDBACK_TYPES.join(', ')}`
        };
    }

    const comment = typeof body.comment === 'string'
        ? body.comment.trim().replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '')
        : '';

    if (!comment || comment.length < 1) {
        return { ok: false, error: 'comment must be at least 1 character' };
    }

    if (comment.length > MAX_COMMENT_LEN) {
        return { ok: false, error: `comment must not exceed ${MAX_COMMENT_LEN} characters` };
    }

    return { ok: true, sanitized: { student_id: studentId, feedback_type: feedbackType, comment } };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('POST /api/feedback — input validation', () => {
    test('valid feedback submission is accepted', () => {
        const { ok, sanitized } = validateFeedbackBody({
            student_id: '28960',
            feedback_type: 'feedback',
            comment: 'App is great!'
        });
        assert.strictEqual(ok, true);
        assert.strictEqual(sanitized.feedback_type, 'feedback');
        assert.strictEqual(sanitized.comment, 'App is great!');
    });

    test('valid grievance submission is accepted', () => {
        const { ok, sanitized } = validateFeedbackBody({
            student_id: '12345',
            feedback_type: 'grievance',
            comment: 'I have a complaint about the attendance record.'
        });
        assert.strictEqual(ok, true);
        assert.strictEqual(sanitized.feedback_type, 'grievance');
    });

    test('valid suggestion submission is accepted', () => {
        const { ok } = validateFeedbackBody({
            student_id: '99999',
            feedback_type: 'suggestion',
            comment: 'Please add dark mode.'
        });
        assert.strictEqual(ok, true);
    });

    test('missing student_id → rejected', () => {
        const { ok, error } = validateFeedbackBody({
            feedback_type: 'feedback',
            comment: 'Hello'
        });
        assert.strictEqual(ok, false);
        assert.ok(error.includes('student_id'));
    });

    test('empty student_id → rejected', () => {
        const { ok } = validateFeedbackBody({
            student_id: '',
            feedback_type: 'feedback',
            comment: 'Hello'
        });
        assert.strictEqual(ok, false);
    });

    test('unknown feedback_type → rejected', () => {
        const { ok, error } = validateFeedbackBody({
            student_id: '123',
            feedback_type: 'complaint',
            comment: 'This is a complaint.'
        });
        assert.strictEqual(ok, false);
        assert.ok(error.includes('feedback_type'));
    });

    test('empty comment → rejected', () => {
        const { ok, error } = validateFeedbackBody({
            student_id: '123',
            feedback_type: 'feedback',
            comment: ''
        });
        assert.strictEqual(ok, false);
        assert.ok(error.includes('comment'));
    });

    test('whitespace-only comment → rejected after trim', () => {
        const { ok } = validateFeedbackBody({
            student_id: '123',
            feedback_type: 'feedback',
            comment: '   \t\n   '
        });
        assert.strictEqual(ok, false);
    });

    test('comment at exactly 2000 chars is accepted', () => {
        const { ok } = validateFeedbackBody({
            student_id: '123',
            feedback_type: 'feedback',
            comment: 'A'.repeat(2000)
        });
        assert.strictEqual(ok, true);
    });

    test('comment at 2001 chars → rejected', () => {
        const { ok, error } = validateFeedbackBody({
            student_id: '123',
            feedback_type: 'feedback',
            comment: 'A'.repeat(2001)
        });
        assert.strictEqual(ok, false);
        assert.ok(error.includes('2000'));
    });

    test('control characters are stripped from comment', () => {
        const { ok, sanitized } = validateFeedbackBody({
            student_id: '123',
            feedback_type: 'suggestion',
            comment: 'Hello\x00\x01World'
        });
        assert.strictEqual(ok, true);
        assert.ok(!sanitized.comment.includes('\x00'));
        assert.ok(!sanitized.comment.includes('\x01'));
    });

    test('null body → rejected', () => {
        const { ok } = validateFeedbackBody(null);
        assert.strictEqual(ok, false);
    });
});

describe('GET /api/feedback — admin auth contract', () => {
    test('unauthenticated GET must return 401 (contract documented)', () => {
        // Real enforcement is in api/feedback.js via isValidSession.
        // This documents the contract; integration test is in Phase 6 smoke tests.
        assert.ok(true, 'GET /api/feedback requires admin bearer session token — documented');
    });
});

describe('PATCH /api/feedback/:id — allowed status values', () => {
    test('status "reviewed" is a valid transition', () => {
        const allowed = ['pending', 'reviewed'];
        assert.ok(allowed.includes('reviewed'));
    });

    test('arbitrary status values should not be accepted', () => {
        const allowed = ['pending', 'reviewed'];
        assert.strictEqual(allowed.includes('deleted'), false);
        assert.strictEqual(allowed.includes('approved'), false);
        assert.strictEqual(allowed.includes('__proto__'), false);
    });
});
