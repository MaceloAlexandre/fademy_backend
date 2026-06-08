'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

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

  const inst = await Institution.create({
    name: 'Test Institution Chat',
    slug: 'test-inst-chat-' + Date.now(),
  });

  const otherInst = await Institution.create({
    name: 'Other Institution Chat',
    slug: 'other-inst-chat-' + Date.now(),
  });

  const passwordHash = await bcrypt.hash('password123', 4);

  const userA = await User.create({
    institutionId: inst._id,
    name: 'User A',
    email: 'user-a-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const userB = await User.create({
    institutionId: inst._id,
    name: 'User B',
    email: 'user-b-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const userC = await User.create({
    institutionId: otherInst._id,
    name: 'User C (other institution)',
    email: 'user-c-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  return { inst, otherInst, userA, userB, userC };
}

describe('conversation.controller', () => {
  const { createConversation, listConversations } = require('./conversation.controller');

  describe('createConversation()', () => {
    it('returns 400 when user tries to message themselves (T-06-01)', async () => {
      const { inst, userA } = await seedUsers();
      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { userId: userA._id.toString() },
      };
      const res = makeRes();

      await createConversation(req, res);

      assert.equal(res.statusCode, 400);
      assert.equal(res.body.error, 'Cannot message yourself');
    });

    it('returns 404 when target user does not exist', async () => {
      const { inst, userA } = await seedUsers();
      const fakeId = new mongoose.Types.ObjectId().toString();
      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { userId: fakeId },
      };
      const res = makeRes();

      await createConversation(req, res);

      assert.equal(res.statusCode, 404);
      assert.equal(res.body.error, 'User not found');
    });

    it('returns 403 when users are not mutual followers (one direction only)', async () => {
      const { inst, userA, userB } = await seedUsers();
      const Follow = require('../models/Follow');

      // Only A follows B, but B does not follow A
      await Follow.create({ followerId: userA._id, followingId: userB._id, status: 'accepted' });

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { userId: userB._id.toString() },
      };
      const res = makeRes();

      await createConversation(req, res);

      assert.equal(res.statusCode, 403);
      assert.equal(res.body.error, 'Both users must follow each other to start a chat');
    });

    it('returns 403 when a block exists (either direction)', async () => {
      const { inst, userA, userB } = await seedUsers();
      const Follow = require('../models/Follow');
      const Block = require('../models/Block');

      // Both follow each other (mutual)
      await Follow.create({ followerId: userA._id, followingId: userB._id, status: 'accepted' });
      await Follow.create({ followerId: userB._id, followingId: userA._id, status: 'accepted' });

      // But A blocks B
      await Block.create({ blockerId: userA._id, blockedId: userB._id });

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { userId: userB._id.toString() },
      };
      const res = makeRes();

      await createConversation(req, res);

      assert.equal(res.statusCode, 403);
      assert.equal(res.body.error, 'Forbidden');
    });

    it('returns 403 when users are from different institutions (T-06-06)', async () => {
      const { inst, userA, userC, otherInst } = await seedUsers();
      const Follow = require('../models/Follow');

      // Both mutually follow each other (accepted) even across institutions
      await Follow.create({ followerId: userA._id, followingId: userC._id, status: 'accepted' });
      await Follow.create({ followerId: userC._id, followingId: userA._id, status: 'accepted' });

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { userId: userC._id.toString() },
      };
      const res = makeRes();

      await createConversation(req, res);

      assert.equal(res.statusCode, 403, 'should return 403 for cross-institution conversation');
    });

    it('returns 201 on first create and 200 (same _id) on repeat — find-or-create (CHAT-01)', async () => {
      const { inst, userA, userB } = await seedUsers();
      const Follow = require('../models/Follow');

      await Follow.create({ followerId: userA._id, followingId: userB._id, status: 'accepted' });
      await Follow.create({ followerId: userB._id, followingId: userA._id, status: 'accepted' });

      const req = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { userId: userB._id.toString() },
      };

      // First create → 201
      const res1 = makeRes();
      await createConversation(req, res1);
      assert.equal(res1.statusCode, 201, 'first create should return 201');
      assert.ok(res1.body.conversation, 'should return conversation object');
      const convId = res1.body.conversation._id.toString();

      // Second create (find-or-create) → 200 with same _id
      const res2 = makeRes();
      await createConversation(req, res2);
      assert.equal(res2.statusCode, 200, 'repeat create should return 200');
      assert.equal(res2.body.conversation._id.toString(), convId, 'should return same conversation _id');
    });
  });

  describe('listConversations()', () => {
    it('returns the created conversation with lastMessage null when no messages', async () => {
      const { inst, userA, userB } = await seedUsers();
      const Follow = require('../models/Follow');

      await Follow.create({ followerId: userA._id, followingId: userB._id, status: 'accepted' });
      await Follow.create({ followerId: userB._id, followingId: userA._id, status: 'accepted' });

      // Create a conversation first
      const createReq = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
        body: { userId: userB._id.toString() },
      };
      const createRes = makeRes();
      await createConversation(createReq, createRes);
      assert.equal(createRes.statusCode, 201);

      // Now list
      const listReq = {
        user: { id: userA._id.toString(), institutionId: inst._id.toString() },
      };
      const listRes = makeRes();
      await listConversations(listReq, listRes);

      assert.equal(listRes.statusCode, 200, 'listConversations should return 200');
      assert.ok(Array.isArray(listRes.body.conversations), 'should return conversations array');
      assert.ok(listRes.body.conversations.length >= 1, 'should have at least one conversation');

      const conv = listRes.body.conversations[0];
      assert.ok(conv.memberIds, 'memberIds should be populated');
      assert.equal(conv.lastMessage, null, 'lastMessage should be null when no messages exist');
    });
  });
});
