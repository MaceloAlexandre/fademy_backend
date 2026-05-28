'use strict';

// Env preamble — must be before any require that transitively loads env.js
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';

const { test, describe } = require('node:test');
const assert = require('node:assert/strict');

// Mock s3.service before loading the controller so no live object storage is needed
const Module = require('module');
const originalLoad = Module._load;
Module._load = function (request, parent, isMain) {
  if (request.includes('s3.service')) {
    return {
      uploadAvatar: async ({ contentType }) => {
        const ext = contentType.split('/')[1];
        const key = `avatars/mock-uuid.${ext}`;
        return { key, objectUrl: `https://eu2.contabostorage.com/tenant:miniredesocial/${key}` };
      },
      objectExists: async () => true,
    };
  }
  return originalLoad.apply(this, arguments);
};

const { upload } = require('./media.controller');

function makeReq(file) {
  return file === undefined ? {} : { file };
}

function makeFile({ mimetype = 'image/png', size = 1000 } = {}) {
  return { mimetype, size, buffer: Buffer.from('fake-image-bytes') };
}

function makeRes() {
  return {
    statusCode: null,
    body: null,
    status(code) { this.statusCode = code; return this; },
    json(data) { this.body = data; return this; },
  };
}

describe('media.controller upload', () => {
  test('valid image/png upload returns 201 with key + objectUrl', async () => {
    const res = makeRes();
    await upload(makeReq(makeFile({ mimetype: 'image/png' })), res);
    assert.equal(res.statusCode, 201);
    assert.ok(res.body.key, 'key must be non-empty');
    assert.ok(res.body.objectUrl, 'objectUrl must be non-empty');
    assert.ok(res.body.objectUrl.includes(res.body.key), 'objectUrl must include the key');
  });

  test('valid image/jpeg upload returns 201', async () => {
    const res = makeRes();
    await upload(makeReq(makeFile({ mimetype: 'image/jpeg', size: 2048 })), res);
    assert.equal(res.statusCode, 201);
  });

  test('valid image/webp upload returns 201', async () => {
    const res = makeRes();
    await upload(makeReq(makeFile({ mimetype: 'image/webp', size: 512 })), res);
    assert.equal(res.statusCode, 201);
  });

  test('missing file returns 400', async () => {
    const res = makeRes();
    await upload(makeReq(), res);
    assert.equal(res.statusCode, 400);
    assert.ok(res.body.error);
  });

  test('disallowed content-type returns 400', async () => {
    const res = makeRes();
    await upload(makeReq(makeFile({ mimetype: 'application/pdf' })), res);
    assert.equal(res.statusCode, 400);
    assert.ok(res.body.error);
  });

  test('size exceeding MAX_UPLOAD_BYTES returns 400', async () => {
    const res = makeRes();
    await upload(makeReq(makeFile({ mimetype: 'image/png', size: 10 * 1024 * 1024 })), res);
    assert.equal(res.statusCode, 400);
    assert.ok(res.body.error);
  });
});
