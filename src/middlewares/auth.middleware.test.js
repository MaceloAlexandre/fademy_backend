'use strict';

const { test, describe, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');

// Use a fixed secret for tests — does not need env
const TEST_SECRET = 'test-secret-for-auth-middleware-tests';

// Stub the env module before loading middleware
// We override JWT_SECRET via env before requiring the module
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = TEST_SECRET;

const authenticate = require('./auth.middleware');
const { signToken, verifyToken } = require('../services/token.service');

// Helper: create mock req/res/next
function makeReq(authHeader) {
  return {
    headers: authHeader ? { authorization: authHeader } : {},
  };
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

// Valid payload for test tokens
const VALID_PAYLOAD = {
  sub: '6474b12345678901234abcde',
  role: 'admin',
  institutionId: '6474b12345678901234abcdf',
};

describe('auth.middleware', () => {
  test('valid Bearer token: calls next() and sets req.user with id, role, institutionId', () => {
    const token = jwt.sign(VALID_PAYLOAD, TEST_SECRET, { algorithm: 'HS256', expiresIn: '1h' });
    const req = makeReq(`Bearer ${token}`);
    const res = makeRes();
    const next = makeNext();

    authenticate(req, res, next);

    assert.ok(next.wasCalled(), 'next() should be called for a valid token');
    assert.ok(req.user, 'req.user should be set');
    assert.equal(req.user.id, VALID_PAYLOAD.sub, 'req.user.id should equal token sub');
    assert.equal(req.user.role, VALID_PAYLOAD.role, 'req.user.role should be set');
    assert.equal(
      req.user.institutionId,
      VALID_PAYLOAD.institutionId,
      'req.user.institutionId should be set'
    );
  });

  test('missing Authorization header: responds 401 and does NOT call next()', () => {
    const req = makeReq(null);
    const res = makeRes();
    const next = makeNext();

    authenticate(req, res, next);

    assert.equal(res.statusCode, 401, 'should respond 401');
    assert.ok(!next.wasCalled(), 'next() should NOT be called');
  });

  test('malformed/tampered token: responds 401 and does NOT call next()', () => {
    const req = makeReq('Bearer totally.not.a.real.jwt');
    const res = makeRes();
    const next = makeNext();

    authenticate(req, res, next);

    assert.equal(res.statusCode, 401, 'should respond 401 for malformed token');
    assert.ok(!next.wasCalled(), 'next() should NOT be called');
  });

  test('expired token: responds 401 and does NOT call next()', () => {
    // Sign a token that expired 1 second ago
    const token = jwt.sign(VALID_PAYLOAD, TEST_SECRET, { algorithm: 'HS256', expiresIn: '-1s' });
    const req = makeReq(`Bearer ${token}`);
    const res = makeRes();
    const next = makeNext();

    authenticate(req, res, next);

    assert.equal(res.statusCode, 401, 'should respond 401 for expired token');
    assert.ok(!next.wasCalled(), 'next() should NOT be called');
  });

  test('alg:none token: responds 401 (algorithm confusion defeated)', () => {
    // Craft a token with alg:none — jsonwebtoken refuses to sign with none,
    // so we craft it manually as base64url parts
    const header = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64url');
    const payload = Buffer.from(JSON.stringify({
      sub: VALID_PAYLOAD.sub,
      role: VALID_PAYLOAD.role,
      institutionId: VALID_PAYLOAD.institutionId,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
    })).toString('base64url');
    const noneToken = `${header}.${payload}.`;

    const req = makeReq(`Bearer ${noneToken}`);
    const res = makeRes();
    const next = makeNext();

    authenticate(req, res, next);

    assert.equal(res.statusCode, 401, 'should reject alg:none token with 401');
    assert.ok(!next.wasCalled(), 'next() should NOT be called for alg:none token');
  });

  test('token signed with wrong algorithm (RS256-like tamper): responds 401', () => {
    // Sign with a different secret — verification will fail
    const token = jwt.sign(VALID_PAYLOAD, 'wrong-secret', { algorithm: 'HS256', expiresIn: '1h' });
    const req = makeReq(`Bearer ${token}`);
    const res = makeRes();
    const next = makeNext();

    authenticate(req, res, next);

    assert.equal(res.statusCode, 401, 'should respond 401 for token signed with wrong secret');
    assert.ok(!next.wasCalled(), 'next() should NOT be called');
  });
});
