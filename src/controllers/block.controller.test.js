'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

// Mock notification.service BEFORE any module loads it.
const Module = require('module');
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) {
    return { create: async () => {} };
  }
  return originalLoad(request, parent, isMain);
};

// NOW require the controller
const { block } = require('./block.controller');

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

async function seedUsersWithFollows() {
  const Institution = require('../models/Institution');
  const User = require('../models/User');
  const Follow = require('../models/Follow');
  const bcrypt = require('bcrypt');
  const { BCRYPT_ROUNDS } = require('../config/env');

  const inst = await Institution.create({
    name: 'Test Institution',
    slug: 'test-inst-block-' + Date.now(),
  });

  const passwordHash = await bcrypt.hash('password123', BCRYPT_ROUNDS);

  const userA = await User.create({
    institutionId: inst._id,
    name: 'User A Block',
    email: 'usera-block-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const userB = await User.create({
    institutionId: inst._id,
    name: 'User B Block',
    email: 'userb-block-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  // Create follow in both directions
  await Follow.create({ followerId: userA._id, followingId: userB._id, status: 'accepted' });
  await Follow.create({ followerId: userB._id, followingId: userA._id, status: 'accepted' });

  return { inst, userA, userB };
}

describe('block.controller', () => {

  describe('block()', () => {
    it('removes both Follow docs and creates Block record (SOCL-04) — returns 200 { blocked: true }', async () => {
      const { userA, userB } = await seedUsersWithFollows();
      const Follow = require('../models/Follow');
      const Block = require('../models/Block');

      const req = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: userB._id.toString() },
      };
      const res = makeRes();

      await block(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.equal(res.body.blocked, true, 'blocked should be true');

      const followCount = await Follow.countDocuments({
        $or: [
          { followerId: userA._id, followingId: userB._id },
          { followerId: userB._id, followingId: userA._id },
        ],
      });
      assert.equal(followCount, 0, 'all bilateral Follow docs should be removed');

      const blockCount = await Block.countDocuments({ blockerId: userA._id, blockedId: userB._id });
      assert.equal(blockCount, 1, 'Block record should exist');
    });

    it('returns 400 when blockerId === blockedId (self-block guard)', async () => {
      const { userA } = await seedUsersWithFollows();

      const req = {
        user: { id: userA._id.toString(), role: 'aluno', institutionId: userA.institutionId.toString() },
        params: { id: userA._id.toString() },
      };
      const res = makeRes();

      await block(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 for self-block');
    });
  });

});
