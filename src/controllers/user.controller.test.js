'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

// Mock notification.service BEFORE any module loads it (same pattern as registration.controller.test.js)
const Module = require('module');
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) {
    return { create: async () => {} };
  }
  return originalLoad(request, parent, isMain);
};

// Require controller after mock is in place
const { getProfile, getMe, updateMe, searchUsers } = require('./user.controller');

const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const mongoose = require('mongoose');

let mongoServer;
let testUri;

// --- DB setup ---

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

// --- helpers ---

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
  const bcrypt = require('bcrypt');

  const inst = await Institution.create({
    name: 'Test Institution ' + Date.now(),
    slug: 'test-inst-' + Date.now(),
  });

  const userA = await User.create({
    institutionId: inst._id,
    name: 'Alice User',
    email: 'alice-' + Date.now() + '@test.com',
    passwordHash: await bcrypt.hash('password123', 4),
    role: 'aluno',
    isPrivate: false,
  });

  const userB = await User.create({
    institutionId: inst._id,
    name: 'Bob User',
    email: 'bob-' + Date.now() + '@test.com',
    passwordHash: await bcrypt.hash('password123', 4),
    role: 'aluno',
    isPrivate: true,
  });

  return { inst, userA, userB };
}

// --- tests ---

describe('user.controller', () => {

  describe('updateMe (PROF-01)', () => {
    it('PATCH /api/users/me with { isPrivate: true } → 200, isPrivate updated to true', async () => {
      const { inst, userA } = await seedData();

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { isPrivate: true },
      };
      const res = makeRes();

      await updateMe(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.equal(res.body.isPrivate, true, 'isPrivate should be true');
      assert.ok(!res.body.passwordHash, 'should not expose passwordHash');
    });

    it('PATCH /api/users/me with non-boolean isPrivate → 400', async () => {
      const { inst, userA } = await seedData();

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { isPrivate: 'yes' },
      };
      const res = makeRes();

      await updateMe(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 for non-boolean');
    });
  });

  describe('getProfile (PROF-02)', () => {
    it('public profile → 200 with followersCount and followingCount', async () => {
      const { inst, userA, userB } = await seedData();

      // userA is public
      const req = {
        user: { id: userB._id.toString(), institutionId: inst._id.toString() },
        params: { id: userA._id.toString() },
      };
      const res = makeRes();

      await getProfile(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.ok('followersCount' in res.body, 'should have followersCount');
      assert.ok('followingCount' in res.body, 'should have followingCount');
      assert.ok(!res.body.passwordHash, 'should not expose passwordHash');
    });

    it('private profile non-follower → 200 with reduced payload (no followersCount)', async () => {
      const { inst, userA, userB } = await seedData();

      // userB is private; userA is the viewer with no follow relationship
      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        params: { id: userB._id.toString() },
      };
      const res = makeRes();

      await getProfile(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.equal(res.body.isPrivate, true, 'isPrivate should be true');
      assert.ok(!('followersCount' in res.body), 'should NOT have followersCount for private non-follower');
      assert.ok(!('followingCount' in res.body), 'should NOT have followingCount for private non-follower');
      assert.ok(!res.body.passwordHash, 'should not expose passwordHash');
    });

    it('blocked user → 404 in either direction', async () => {
      const { inst, userA, userB } = await seedData();
      const Block = require('../models/Block');

      // userA blocks userB
      await Block.create({ blockerId: userA._id, blockedId: userB._id });

      // userB tries to view userA — should see 404
      const req = {
        user: { id: userB._id.toString(), institutionId: inst._id.toString() },
        params: { id: userA._id.toString() },
      };
      const res = makeRes();

      await getProfile(req, res);

      assert.equal(res.statusCode, 404, 'blocked user should see 404');
    });
  });

  describe('getMe (PROF-02 isOwner)', () => {
    it('GET /api/users/me → 200 with caller own profile', async () => {
      const { inst, userA } = await seedData();

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        params: {},
      };
      const res = makeRes();

      await getMe(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.equal(res.body.id.toString(), userA._id.toString(), 'should return caller own profile');
      assert.ok(!res.body.passwordHash, 'should not expose passwordHash');
    });
  });

  describe('searchUsers (PROF-03, SOCL-05)', () => {
    it('q=User → 200 with users array containing matching users', async () => {
      const { inst, userA } = await seedData();

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        query: { q: 'User' },
      };
      const res = makeRes();

      await searchUsers(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.ok(Array.isArray(res.body.users), 'users should be an array');
      assert.ok(res.body.users.length > 0, 'should return matching users');
      assert.ok(!res.body.users.some((u) => u.passwordHash), 'should not expose passwordHash');
    });

    it('empty q → 200 with empty users array (PROF-03 Pitfall 6)', async () => {
      const { inst, userA } = await seedData();

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        query: { q: '' },
      };
      const res = makeRes();

      await searchUsers(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.deepEqual(res.body.users, [], 'should return empty array for empty q');
      assert.equal(res.body.total, 0, 'total should be 0');
    });

    it('SOCL-05: search excludes user who has blocked the viewer', async () => {
      const { inst, userA, userB } = await seedData();
      const Block = require('../models/Block');

      // userB blocks userA (userA is the viewer)
      await Block.create({ blockerId: userB._id, blockedId: userA._id });

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        query: { q: 'Bob' },
      };
      const res = makeRes();

      await searchUsers(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      const ids = res.body.users.map((u) => u._id.toString());
      assert.ok(!ids.includes(userB._id.toString()), 'blocker should be excluded from search results');
    });
  });

});
