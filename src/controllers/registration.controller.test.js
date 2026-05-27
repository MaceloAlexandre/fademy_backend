'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4'; // fast for tests

// Require the controller at the top so MODULE_NOT_FOUND is an unambiguous RED signal
// before the controller exists.
const { register } = require('./registration.controller');

const { describe, it, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const mongoose = require('mongoose');
const Module = require('module');

// --- S3 service mock (avoids live AWS) ---
// We intercept require('...s3.service') before any module loads it
let s3ObjectExistsReturnValue = true;
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('s3.service')) {
    return {
      getPresignedPutUrl: async () => ({ uploadUrl: 'https://mock.s3.amazonaws.com/presigned', key: 'avatars/mock-uuid.jpg', objectUrl: 'https://mock-bucket.s3.us-east-1.amazonaws.com/avatars/mock-uuid.jpg' }),
      objectExists: async () => s3ObjectExistsReturnValue,
    };
  }
  return originalLoad(request, parent, isMain);
};

// --- DB setup ---

let mongoServer;
let testUri;

before(async () => {
  let MongoMemoryServer;
  try {
    ({ MongoMemoryServer } = require('mongodb-memory-server'));
  } catch (err) {
    console.log('SKIP: mongodb-memory-server not available');
    process.exit(0);
  }

  const uri = process.env.MONGODB_URI_TEST;
  if (uri) {
    testUri = uri;
  } else {
    mongoServer = await MongoMemoryServer.create();
    testUri = mongoServer.getUri();
  }

  process.env.MONGODB_URI = testUri;

  const { connectDB } = require('../config/db');
  await connectDB(testUri);
});

after(async () => {
  Module._load = originalLoad;
  await mongoose.disconnect();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

// --- Helpers ---

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

async function seedData() {
  const Institution = require('../models/Institution');
  const User = require('../models/User');
  const Invite = require('../models/Invite');
  const bcrypt = require('bcrypt');

  const inst = await Institution.create({
    name: 'Test University ' + Date.now(),
    slug: 'test-uni-' + Date.now(),
  });

  const inviter = await User.create({
    institutionId: inst._id,
    name: 'Prof Inviter',
    email: 'inviter-' + Date.now() + '@test.com',
    passwordHash: await bcrypt.hash('password123', 4),
    role: 'professor',
  });

  const futureDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
  const pastDate = new Date(Date.now() - 1000);

  const activeInvite = await Invite.create({
    institutionId: inst._id,
    invitedById: inviter._id,
    roleTarget: 'aluno',
    token: 'valid-token-' + Date.now(),
    maxUses: 10,
    usesCount: 0,
    status: 'active',
    expiresAt: futureDate,
  });

  const expiredInvite = await Invite.create({
    institutionId: inst._id,
    invitedById: inviter._id,
    roleTarget: 'aluno',
    token: 'expired-token-' + Date.now(),
    maxUses: 10,
    usesCount: 0,
    status: 'active', // status is active but expiresAt is past
    expiresAt: pastDate,
  });

  const revokedInvite = await Invite.create({
    institutionId: inst._id,
    invitedById: inviter._id,
    roleTarget: 'aluno',
    token: 'revoked-token-' + Date.now(),
    maxUses: 10,
    usesCount: 0,
    status: 'revoked',
    expiresAt: futureDate,
  });

  const fullInvite = await Invite.create({
    institutionId: inst._id,
    invitedById: inviter._id,
    roleTarget: 'aluno',
    token: 'full-token-' + Date.now(),
    maxUses: 5,
    usesCount: 5, // already at capacity
    status: 'active',
    expiresAt: futureDate,
  });

  // Invite with only 1 slot left (to test last-slot → expired behavior)
  const lastSlotInvite = await Invite.create({
    institutionId: inst._id,
    invitedById: inviter._id,
    roleTarget: 'aluno',
    token: 'last-slot-token-' + Date.now(),
    maxUses: 1,
    usesCount: 0,
    status: 'active',
    expiresAt: futureDate,
  });

  return { inst, inviter, activeInvite, expiredInvite, revokedInvite, fullInvite, lastSlotInvite };
}

// --- Tests ---

describe('registration.controller', () => {

  describe('register — happy path', () => {
    it('201: creates user with correct role/institution/avatarUrl when valid invite token + confirmed S3 object', async () => {
      const { inst, activeInvite } = await seedData();
      const User = require('../models/User');
      const Invite = require('../models/Invite');

      s3ObjectExistsReturnValue = true;
      const email = 'new-user-' + Date.now() + '@test.com';
      const req = {
        body: {
          token: activeInvite.token,
          name: 'New Student',
          email,
          password: 'Password123!',
          avatarUrl: 'https://mock-bucket.s3.us-east-1.amazonaws.com/avatars/some-uuid.jpg',
          avatarKey: 'avatars/some-uuid.jpg',
        },
      };
      const res = makeRes();

      await register(req, res);

      assert.equal(res.statusCode, 201, 'should return 201');

      // User persisted with correct scoping
      const user = await User.findOne({ email });
      assert.ok(user, 'user should be persisted');
      assert.equal(user.role, 'aluno', 'role should match invite.roleTarget');
      assert.equal(user.institutionId.toString(), inst._id.toString(), 'institutionId should match invite');
      assert.ok(user.avatarUrl, 'avatarUrl should be set');
      assert.ok(user.passwordHash, 'passwordHash should be set');
      assert.notEqual(user.passwordHash, 'Password123!', 'passwordHash should NOT be plaintext');
      assert.match(user.passwordHash, /^\$2[ab]\$/, 'passwordHash should be bcrypt');

      // usesCount incremented
      const updatedInvite = await Invite.findById(activeInvite._id);
      assert.equal(updatedInvite.usesCount, 1, 'usesCount should be incremented');

      // Response does not contain a token (no auto-login)
      assert.ok(!res.body.token, 'response should NOT contain a JWT token');
    });

    it('201: response contains id, name, email, role, institutionId, avatarUrl — no passwordHash', async () => {
      const { activeInvite } = await seedData();

      s3ObjectExistsReturnValue = true;
      const email = 'check-resp-' + Date.now() + '@test.com';
      const req = {
        body: {
          token: activeInvite.token,
          name: 'Check Response',
          email,
          password: 'Password123!',
          avatarUrl: 'https://mock-bucket.s3.us-east-1.amazonaws.com/avatars/photo.jpg',
          avatarKey: 'avatars/photo.jpg',
        },
      };
      const res = makeRes();

      await register(req, res);

      assert.equal(res.statusCode, 201);
      const body = res.body;
      assert.ok(body.id || body._id || (body.user && (body.user.id || body.user._id)), 'response should have user id');
      assert.ok(!body.passwordHash, 'response must NOT expose passwordHash');
      if (body.user) {
        assert.ok(!body.user.passwordHash, 'response user must NOT expose passwordHash');
      }
    });
  });

  describe('register — invite validation', () => {
    it('400: expired invite (expiresAt in the past) — no user created', async () => {
      const { inst, expiredInvite } = await seedData();
      const User = require('../models/User');
      const countBefore = await User.countDocuments({ institutionId: inst._id });

      s3ObjectExistsReturnValue = true;
      const req = {
        body: {
          token: expiredInvite.token,
          name: 'Should Fail',
          email: 'fail-expired-' + Date.now() + '@test.com',
          password: 'Password123!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo.jpg',
          avatarKey: 'avatars/photo.jpg',
        },
      };
      const res = makeRes();

      await register(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 for expired invite');
      const countAfter = await User.countDocuments({ institutionId: inst._id });
      assert.equal(countAfter, countBefore, 'no user should be created');
    });

    it('400: revoked invite — no user created', async () => {
      const { inst, revokedInvite } = await seedData();
      const User = require('../models/User');
      const countBefore = await User.countDocuments({ institutionId: inst._id });

      s3ObjectExistsReturnValue = true;
      const req = {
        body: {
          token: revokedInvite.token,
          name: 'Should Fail',
          email: 'fail-revoked-' + Date.now() + '@test.com',
          password: 'Password123!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo.jpg',
          avatarKey: 'avatars/photo.jpg',
        },
      };
      const res = makeRes();

      await register(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 for revoked invite');
      const countAfter = await User.countDocuments({ institutionId: inst._id });
      assert.equal(countAfter, countBefore, 'no user should be created');
    });

    it('400: invite at max capacity (usesCount === maxUses) — no user created', async () => {
      const { inst, fullInvite } = await seedData();
      const User = require('../models/User');
      const countBefore = await User.countDocuments({ institutionId: inst._id });

      s3ObjectExistsReturnValue = true;
      const req = {
        body: {
          token: fullInvite.token,
          name: 'Should Fail',
          email: 'fail-full-' + Date.now() + '@test.com',
          password: 'Password123!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo.jpg',
          avatarKey: 'avatars/photo.jpg',
        },
      };
      const res = makeRes();

      await register(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 when invite is full');
      const countAfter = await User.countDocuments({ institutionId: inst._id });
      assert.equal(countAfter, countBefore, 'no user should be created');
    });
  });

  describe('register — avatar / S3 validation', () => {
    it('400: missing avatarKey — no user created', async () => {
      const { activeInvite, inst } = await seedData();
      const User = require('../models/User');
      const countBefore = await User.countDocuments({ institutionId: inst._id });

      const req = {
        body: {
          token: activeInvite.token,
          name: 'Should Fail',
          email: 'fail-nokey-' + Date.now() + '@test.com',
          password: 'Password123!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo.jpg',
          // avatarKey intentionally omitted
        },
      };
      const res = makeRes();

      await register(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 when avatarKey is missing');
      const countAfter = await User.countDocuments({ institutionId: inst._id });
      assert.equal(countAfter, countBefore, 'no user should be created');
    });

    it('400: objectExists returns false (S3 object not found) — no user created', async () => {
      const { activeInvite, inst } = await seedData();
      const User = require('../models/User');
      const countBefore = await User.countDocuments({ institutionId: inst._id });

      s3ObjectExistsReturnValue = false;
      const req = {
        body: {
          token: activeInvite.token,
          name: 'Should Fail',
          email: 'fail-noobj-' + Date.now() + '@test.com',
          password: 'Password123!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo.jpg',
          avatarKey: 'avatars/photo.jpg',
        },
      };
      const res = makeRes();

      await register(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 when S3 object does not exist');
      const countAfter = await User.countDocuments({ institutionId: inst._id });
      assert.equal(countAfter, countBefore, 'no user should be created');
      s3ObjectExistsReturnValue = true;
    });
  });

  describe('register — duplicate email', () => {
    it('409: duplicate email within same institution — no second user created', async () => {
      const { activeInvite, inst } = await seedData();
      const User = require('../models/User');

      s3ObjectExistsReturnValue = true;
      const email = 'dup-email-' + Date.now() + '@test.com';

      // First registration — should succeed
      const req1 = {
        body: {
          token: activeInvite.token,
          name: 'First User',
          email,
          password: 'Password123!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo1.jpg',
          avatarKey: 'avatars/photo1.jpg',
        },
      };
      const res1 = makeRes();
      await register(req1, res1);
      assert.equal(res1.statusCode, 201, 'first registration should succeed');

      // Second registration with same email — should fail
      const req2 = {
        body: {
          token: activeInvite.token,
          name: 'Second User',
          email,
          password: 'Password456!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo2.jpg',
          avatarKey: 'avatars/photo2.jpg',
        },
      };
      const res2 = makeRes();
      await register(req2, res2);

      assert.equal(res2.statusCode, 409, 'duplicate email in same institution should return 409');
      const userCount = await User.countDocuments({ email, institutionId: inst._id });
      assert.equal(userCount, 1, 'only one user should exist with that email in the institution');
    });
  });

  describe('register — slot consumption', () => {
    it('invite status flips to expired when last slot is consumed', async () => {
      const { lastSlotInvite } = await seedData();
      const Invite = require('../models/Invite');

      s3ObjectExistsReturnValue = true;
      const req = {
        body: {
          token: lastSlotInvite.token,
          name: 'Last Slot User',
          email: 'last-slot-' + Date.now() + '@test.com',
          password: 'Password123!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo.jpg',
          avatarKey: 'avatars/photo.jpg',
        },
      };
      const res = makeRes();

      await register(req, res);

      assert.equal(res.statusCode, 201, 'should succeed for last slot');

      const updatedInvite = await Invite.findById(lastSlotInvite._id);
      assert.equal(updatedInvite.usesCount, 1, 'usesCount should be 1');
      assert.equal(updatedInvite.status, 'expired', 'status should be expired after last slot consumed');

      // Next attempt on now-expired invite should fail
      const req2 = {
        body: {
          token: lastSlotInvite.token,
          name: 'Over Limit',
          email: 'over-limit-' + Date.now() + '@test.com',
          password: 'Password123!',
          avatarUrl: 'https://mock.s3.amazonaws.com/avatars/photo2.jpg',
          avatarKey: 'avatars/photo2.jpg',
        },
      };
      const res2 = makeRes();
      await register(req2, res2);
      assert.equal(res2.statusCode, 400, 'should reject when invite is now expired/closed');
    });
  });

});
