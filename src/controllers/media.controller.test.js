'use strict';

// Env preamble — must be before any require that transitively loads env.js
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';

const { test, describe, mock } = require('node:test');
const assert = require('node:assert/strict');

// Mock s3.service before loading the controller
const MOCK_UPLOAD_URL = 'https://fademy-dev.s3.us-east-1.amazonaws.com/avatars/mock-uuid.png?X-Amz-Signature=abc';
const MOCK_KEY = 'avatars/mock-uuid.png';
const MOCK_OBJECT_URL = 'https://fademy-dev.s3.us-east-1.amazonaws.com/avatars/mock-uuid.png';

// Register the mock before loading the controller
const Module = require('module');
const originalLoad = Module._load;
Module._load = function (request, parent, isMain) {
  if (request.includes('s3.service')) {
    return {
      getPresignedPutUrl: async ({ key, contentType }) => ({
        uploadUrl: MOCK_UPLOAD_URL,
        key,
        // objectUrl uses the server-generated key so the test can verify objectUrl.includes(key)
        objectUrl: `https://fademy-dev.s3.us-east-1.amazonaws.com/${key}`,
      }),
      objectExists: async (key) => key === MOCK_KEY,
    };
  }
  return originalLoad.apply(this, arguments);
};

const { presign } = require('./media.controller');

// Helpers
function makeReq(body) {
  return { body: body || {} };
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

describe('media.controller presign', () => {
  test('valid image/png request returns 200 with uploadUrl, key, objectUrl', async () => {
    const req = makeReq({ contentType: 'image/png', size: 1000 });
    const res = makeRes();
    await presign(req, res);
    assert.equal(res.statusCode, 200);
    assert.ok(res.body.uploadUrl, 'uploadUrl must be non-empty');
    assert.ok(res.body.key, 'key must be non-empty');
    assert.ok(res.body.objectUrl, 'objectUrl must be non-empty');
    // objectUrl must contain the bucket and the returned key
    assert.ok(res.body.objectUrl.includes(res.body.key), 'objectUrl must include the key');
  });

  test('valid image/jpeg request returns 200', async () => {
    const req = makeReq({ contentType: 'image/jpeg', size: 2048 });
    const res = makeRes();
    await presign(req, res);
    assert.equal(res.statusCode, 200);
    assert.ok(res.body.uploadUrl);
  });

  test('valid image/webp request returns 200', async () => {
    const req = makeReq({ contentType: 'image/webp', size: 512 });
    const res = makeRes();
    await presign(req, res);
    assert.equal(res.statusCode, 200);
  });

  test('disallowed content-type returns 400', async () => {
    const req = makeReq({ contentType: 'application/pdf', size: 1000 });
    const res = makeRes();
    await presign(req, res);
    assert.equal(res.statusCode, 400);
    assert.ok(res.body.error);
  });

  test('size exceeding MAX_UPLOAD_BYTES returns 400', async () => {
    const req = makeReq({ contentType: 'image/png', size: 10 * 1024 * 1024 });
    const res = makeRes();
    await presign(req, res);
    assert.equal(res.statusCode, 400);
    assert.ok(res.body.error);
  });

  test('missing contentType returns 400', async () => {
    const req = makeReq({ size: 1000 });
    const res = makeRes();
    await presign(req, res);
    assert.equal(res.statusCode, 400);
    assert.ok(res.body.error);
  });

  test('missing size returns 400', async () => {
    const req = makeReq({ contentType: 'image/png' });
    const res = makeRes();
    await presign(req, res);
    assert.equal(res.statusCode, 400);
    assert.ok(res.body.error);
  });

  test('empty body returns 400', async () => {
    const req = makeReq({});
    const res = makeRes();
    await presign(req, res);
    assert.equal(res.statusCode, 400);
    assert.ok(res.body.error);
  });
});
