'use strict';

const { test, describe } = require('node:test');
const assert = require('node:assert/strict');

process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';

const { requireRole } = require('./role.middleware');

// Helpers
function makeReqWithUser(user) {
  return { user };
}

function makeRes() {
  const res = {
    statusCode: null,
    body: null,
    status(code) {
      this.statusCode = code;
      return this;
    },
    json(data) {
      this.body = data;
      return this;
    },
  };
  return res;
}

function makeNext() {
  let called = false;
  const fn = () => { called = true; };
  fn.wasCalled = () => called;
  return fn;
}

describe('role.middleware (requireRole)', () => {
  test('allowed role: calls next()', () => {
    const req = makeReqWithUser({ id: 'u1', role: 'admin', institutionId: 'inst1' });
    const res = makeRes();
    const next = makeNext();

    requireRole('admin')(req, res, next);

    assert.ok(next.wasCalled(), 'next() should be called when role is allowed');
    assert.equal(res.statusCode, null, 'should not set status code when allowed');
  });

  test('disallowed role: responds 403 and does NOT call next()', () => {
    const req = makeReqWithUser({ id: 'u2', role: 'aluno', institutionId: 'inst1' });
    const res = makeRes();
    const next = makeNext();

    requireRole('admin')(req, res, next);

    assert.equal(res.statusCode, 403, 'should respond 403 for disallowed role');
    assert.ok(!next.wasCalled(), 'next() should NOT be called for disallowed role');
  });

  test('multiple allowed roles: calls next() when user has one of the allowed roles', () => {
    const req = makeReqWithUser({ id: 'u3', role: 'professor', institutionId: 'inst1' });
    const res = makeRes();
    const next = makeNext();

    requireRole('admin', 'coordenador', 'professor')(req, res, next);

    assert.ok(next.wasCalled(), 'next() should be called when role is in allowed list');
  });

  test('no req.user (auth not run): fails closed — 401 and does NOT call next()', () => {
    const req = { headers: {} }; // no user property
    const res = makeRes();
    const next = makeNext();

    requireRole('admin')(req, res, next);

    // Should fail closed — 401 (unauthenticated) or 403, never pass through
    const statusCode = res.statusCode;
    assert.ok(
      statusCode === 401 || statusCode === 403,
      `should respond 401 or 403 when req.user is missing, got ${statusCode}`
    );
    assert.ok(!next.wasCalled(), 'next() should NOT be called when req.user is absent');
  });

  test('req.user.role undefined: fails closed — does NOT call next()', () => {
    const req = makeReqWithUser({ id: 'u4', institutionId: 'inst1' }); // role missing
    const res = makeRes();
    const next = makeNext();

    requireRole('admin')(req, res, next);

    assert.ok(!next.wasCalled(), 'next() should NOT be called when role is undefined');
    assert.ok(res.statusCode >= 400, 'should respond with 4xx error');
  });
});
