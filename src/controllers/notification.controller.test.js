'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

const { listNotifications, markRead } = require('./notification.controller');

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

async function seedFixture() {
  const Institution = require('../models/Institution');
  const User = require('../models/User');
  const Notification = require('../models/Notification');
  const bcrypt = require('bcrypt');
  const { BCRYPT_ROUNDS } = require('../config/env');

  const inst = await Institution.create({
    name: 'Test Institution Notifications',
    slug: 'test-inst-notifications-' + Date.now() + '-' + Math.random().toString(36).slice(2),
  });

  const passwordHash = await bcrypt.hash('password123', BCRYPT_ROUNDS);

  const userA = await User.create({
    institutionId: inst._id,
    name: 'User A',
    email: 'usera-notif-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const userB = await User.create({
    institutionId: inst._id,
    name: 'User B',
    email: 'userb-notif-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const actor = await User.create({
    institutionId: inst._id,
    name: 'Actor User',
    email: 'actor-notif-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  // Notifications for userA — older first, then newer (so sort _id:-1 puts newer first)
  const notifA1 = await Notification.create({
    userId: userA._id,
    actorId: actor._id,
    type: 'new_follower',
    refId: actor._id,
    refModel: 'User',
    read: false,
  });

  const notifA2 = await Notification.create({
    userId: userA._id,
    actorId: actor._id,
    type: 'follow_accepted',
    refId: actor._id,
    refModel: 'User',
    read: false,
  });

  // Notification belonging to userB — must never be visible/mutable by userA
  const notifB1 = await Notification.create({
    userId: userB._id,
    actorId: actor._id,
    type: 'new_follower',
    refId: actor._id,
    refModel: 'User',
    read: false,
  });

  return { inst, userA, userB, actor, notifA1, notifA2, notifB1 };
}

describe('notification.controller', () => {

  describe('listNotifications()', () => {
    it('returns 200 with caller-scoped notifications, most-recent-first, and an unreadCount (NOTF-03/NOTF-04)', async () => {
      const { userA, notifA1, notifA2 } = await seedFixture();

      const req = {
        user: { id: userA._id.toString(), institutionId: userA.institutionId.toString() },
        query: {},
        params: {},
      };
      const res = makeRes();

      await listNotifications(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.ok(Array.isArray(res.body.notifications), 'should return notifications array');
      assert.ok(res.body.notifications.length >= 2, 'should have at least 2 notifications for userA');
      assert.equal(typeof res.body.unreadCount, 'number', 'unreadCount should be a number');
      assert.ok(res.body.unreadCount >= 2, 'unreadCount should reflect unread notifications');

      // Most-recent-first: notifA2 (created after notifA1) should come before notifA1
      const ids = res.body.notifications.map(n => n._id.toString());
      const idxA2 = ids.indexOf(notifA2._id.toString());
      const idxA1 = ids.indexOf(notifA1._id.toString());
      assert.ok(idxA2 < idxA1, 'most recently created notification should appear first');

      // actorId populated
      assert.ok(res.body.notifications[0].actorId && res.body.notifications[0].actorId.name, 'actorId should be populated with name');
    });

    it('never returns another user\'s notifications (T-05-01 — cross-user isolation)', async () => {
      const { userA, notifB1 } = await seedFixture();

      const req = {
        user: { id: userA._id.toString(), institutionId: userA.institutionId.toString() },
        query: {},
        params: {},
      };
      const res = makeRes();

      await listNotifications(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      const ids = res.body.notifications.map(n => n._id.toString());
      assert.ok(!ids.includes(notifB1._id.toString()), 'userB notification must not appear in userA list');
    });
  });

  describe('markRead()', () => {
    it('returns 403 when caller is not the notification owner (T-05-02 — IDOR guard)', async () => {
      const { userA, notifB1 } = await seedFixture();

      const req = {
        user: { id: userA._id.toString(), institutionId: userA.institutionId.toString() },
        params: { id: notifB1._id.toString() },
      };
      const res = makeRes();

      await markRead(req, res);

      assert.equal(res.statusCode, 403, 'should return 403 when caller is not the owner');
    });

    it('returns 404 when the notification does not exist', async () => {
      const { userA } = await seedFixture();
      const missingId = new mongoose.Types.ObjectId().toString();

      const req = {
        user: { id: userA._id.toString(), institutionId: userA.institutionId.toString() },
        params: { id: missingId },
      };
      const res = makeRes();

      await markRead(req, res);

      assert.equal(res.statusCode, 404, 'should return 404 for missing notification');
    });

    it('returns 200 and persists read=true for the owner (NOTF-04)', async () => {
      const { userA, notifA1 } = await seedFixture();
      const Notification = require('../models/Notification');

      const req = {
        user: { id: userA._id.toString(), institutionId: userA.institutionId.toString() },
        params: { id: notifA1._id.toString() },
      };
      const res = makeRes();

      await markRead(req, res);

      assert.equal(res.statusCode, 200, 'should return 200 for owner');
      assert.equal(res.body.read, true, 'response should report read:true');

      const persisted = await Notification.findById(notifA1._id);
      assert.equal(persisted.read, true, 'read=true should be persisted');
    });
  });

});
