'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

// Mock notification.service BEFORE any module (including the controller) loads it.
const Module = require('module');
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) {
    return { create: async () => {} };
  }
  return originalLoad(request, parent, isMain);
};

// NOW require the controller
const { follow, unfollow, acceptFollow, rejectFollow } = require('./follow.controller');

const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const mongoose = require('mongoose');

let mongoServer;
let testUri;

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
  await mongoose.disconnect();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

async function seedUsers() {
  const Institution = require('../models/Institution');
  const User = require('../models/User');
  const bcrypt = require('bcrypt');
  const { BCRYPT_ROUNDS } = require('../config/env');

  const inst = await Institution.create({
    name: 'Test Institution',
    slug: 'test-inst-follow-' + Date.now(),
  });

  const passwordHash = await bcrypt.hash('password123', BCRYPT_ROUNDS);

  const userA = await User.create({
    institutionId: inst._id,
    name: 'User A',
    email: 'usera-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const userB = await User.create({
    institutionId: inst._id,
    name: 'User B',
    email: 'userb-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: true,
  });

  return { inst, userA, userB };
}

describe('follow.controller', () => {

  describe('follow()', () => {
    it('creates Follow{status:accepted} for public profile (SOCL-01) — returns 201 with status accepted', async () => {
      const { userA, userB } = await seedUsers();
      // userA follows userB... but userB is private. Let's follow userA (public) from userB perspective
      // userA is public (isPrivate:false), userB is private (isPrivate:true)
      // Test: userB follows userA → should be accepted (userA is public)
      const req = {
        user: { id: userB._id.toString(), role: 'aluno', institutionId: userB.institutionId.toString() },
        params: { id: userA._id.toString() },
      };
      const res = makeRes();

      await follow(req, res);

      assert.equal(res.statusCode, 201, 'should return 201');
      assert.equal(res.body.status, 'accepted', 'status should be accepted for public profile');
      assert.ok(res.body.followId, 'should return followId');
    });

    it('creates Follow{status:pending} for private profile (SOCL-02) — returns 201 with status pending', async () => {
      const { userA, userB } = await seedUsers();
      // userA follows userB (private) → should be pending
      const req = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: userB._id.toString() },
      };
      const res = makeRes();

      await follow(req, res);

      assert.equal(res.statusCode, 201, 'should return 201');
      assert.equal(res.body.status, 'pending', 'status should be pending for private profile');
    });

    it('returns 400 when followerId === followingId (self-follow guard)', async () => {
      const { userA } = await seedUsers();
      const req = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: userA._id.toString() },
      };
      const res = makeRes();

      await follow(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 for self-follow');
    });
  });

  describe('unfollow()', () => {
    it('returns 200 { unfollowed: true } after a successful follow', async () => {
      const { userA, userB } = await seedUsers();

      // First follow
      const reqFollow = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: userB._id.toString() },
      };
      const resFollow = makeRes();
      await follow(reqFollow, resFollow);
      assert.equal(resFollow.statusCode, 201, 'follow should succeed first');

      // Then unfollow
      const reqUnfollow = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: userB._id.toString() },
      };
      const resUnfollow = makeRes();
      await unfollow(reqUnfollow, resUnfollow);

      assert.equal(resUnfollow.statusCode, 200, 'unfollow should return 200');
      assert.equal(resUnfollow.body.unfollowed, true, 'unfollowed should be true');
    });
  });

  describe('acceptFollow()', () => {
    it('returns 403 when caller is NOT the follow target (IDOR protection — SOCL-03)', async () => {
      const { userA, userB } = await seedUsers();

      // userA follows userB (private) — creates pending request
      const reqFollow = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: userB._id.toString() },
      };
      const resFollow = makeRes();
      await follow(reqFollow, resFollow);
      assert.equal(resFollow.statusCode, 201, 'follow should succeed');
      const followId = resFollow.body.followId;

      // userA (the follower, NOT the target) tries to accept → should 403
      const reqAccept = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: followId.toString() },
      };
      const resAccept = makeRes();
      await acceptFollow(reqAccept, resAccept);

      assert.equal(resAccept.statusCode, 403, 'should return 403 when caller is not the follow target');
    });

    it('returns 200 and status:accepted when caller IS the follow target (SOCL-03)', async () => {
      const { userA, userB } = await seedUsers();

      // userA follows userB (private) — creates pending request
      const reqFollow = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: userB._id.toString() },
      };
      const resFollow = makeRes();
      await follow(reqFollow, resFollow);
      assert.equal(resFollow.statusCode, 201, 'follow should succeed');
      const followId = resFollow.body.followId;

      // userB (the target) accepts → should 200
      const reqAccept = {
        user: { id: userB._id.toString(), role: 'aluno', institutionId: userB.institutionId.toString() },
        params: { id: followId.toString() },
      };
      const resAccept = makeRes();
      await acceptFollow(reqAccept, resAccept);

      assert.equal(resAccept.statusCode, 200, 'should return 200');
      assert.equal(resAccept.body.status, 'accepted', 'status should be accepted');
    });
  });

});
