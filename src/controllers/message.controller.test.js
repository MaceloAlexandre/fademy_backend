'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

// Spy on notificationService.create to track calls AND capture args.
// Intercept BEFORE requiring the controller so the controller gets the spy object.
const notifSpy = {
  calls: 0,
  lastArgs: null,
  create: async function (...args) {
    notifSpy.calls += 1;
    notifSpy.lastArgs = args;
  },
};

// S3 stub: intercept s3.service so no real AWS calls are made
const s3Stub = {
  uploadMessageImage: async ({ buffer, contentType }) => ({
    key: 'messages/x.jpg',
    objectUrl: 'https://s3/messages/x.jpg',
  }),
};

const Module = require('module');
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) {
    return notifSpy;
  }
  if (request.includes('s3.service')) {
    return s3Stub;
  }
  return originalLoad(request, parent, isMain);
};

// NOW require the controller (gets the spies)
const { createMessage, listMessages } = require('./message.controller');

const { describe, it, before, after, beforeEach } = require('node:test');
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

// Socket stub: captures emit calls
function makeIoStub() {
  const spy = { emitted: [] };
  spy.to = (roomId) => ({
    emit: (event, data) => {
      spy.emitted.push({ roomId, event, data });
    },
  });
  return spy;
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

async function seedConversation() {
  const Institution = require('../models/Institution');
  const User = require('../models/User');
  const Conversation = require('../models/Conversation');
  const bcrypt = require('bcrypt');
  const { BCRYPT_ROUNDS } = require('../config/env');

  const inst = await Institution.create({
    name: 'Test Inst Msg ' + Date.now(),
    slug: 'test-inst-msg-' + Date.now(),
  });

  const passwordHash = await bcrypt.hash('password123', BCRYPT_ROUNDS);

  const sender = await User.create({
    institutionId: inst._id,
    name: 'Sender',
    email: 'sender-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const recipient = await User.create({
    institutionId: inst._id,
    name: 'Recipient',
    email: 'recipient-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const outsider = await User.create({
    institutionId: inst._id,
    name: 'Outsider',
    email: 'outsider-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const conversation = await Conversation.create({
    memberIds: [sender._id, recipient._id],
    isGroup: false,
    createdById: sender._id,
  });

  return { inst, sender, recipient, outsider, conversation };
}

describe('message.controller', () => {

  describe('createMessage()', () => {

    it('member sends text-only message → 201, message persisted, notification dispatched with conversationId refId', async () => {
      const { sender, recipient, conversation } = await seedConversation();
      const Message = require('../models/Message');

      notifSpy.calls = 0;
      notifSpy.lastArgs = null;
      const ioStub = makeIoStub();

      const req = {
        user: { id: sender._id.toString() },
        params: { id: conversation._id.toString() },
        body: { text: 'Hello world' },
        files: [],
        app: { get: () => ioStub },
      };
      const res = makeRes();

      await createMessage(req, res);

      assert.equal(res.statusCode, 201, 'should return 201');
      assert.ok(res.body.message, 'should return message object');
      assert.equal(res.body.message.text, 'Hello world');

      const persisted = await Message.findById(res.body.message._id);
      assert.ok(persisted, 'message should be persisted in DB');
      assert.equal(persisted.text, 'Hello world');

      // Notification spy: called once with correct args
      assert.equal(notifSpy.calls, 1, 'notification service should be called once');
      assert.equal(notifSpy.lastArgs[0], 'message_received', 'type should be message_received');
      assert.equal(notifSpy.lastArgs[1].toString(), sender._id.toString(), 'actorId should be sender');
      assert.equal(notifSpy.lastArgs[2].toString(), recipient._id.toString(), 'userId should be recipient');
      // refId must be conversationId (NOT message._id) — Plan 04 deep-link depends on this
      assert.equal(notifSpy.lastArgs[3].toString(), conversation._id.toString(), 'refId must be conversationId');
      assert.equal(notifSpy.lastArgs[4], 'Conversation', 'refModel must be Conversation');

      // Socket emit
      assert.equal(ioStub.emitted.length, 1, 'socket emit should fire once');
      assert.equal(ioStub.emitted[0].event, 'message:new', 'socket event should be message:new');
    });

    it('member sends image-only (no text) → 201, mediaUrls length 1', async () => {
      const { sender, conversation } = await seedConversation();

      const ioStub = makeIoStub();
      const req = {
        user: { id: sender._id.toString() },
        params: { id: conversation._id.toString() },
        body: {},
        files: [{ buffer: Buffer.from('fake-image'), mimetype: 'image/jpeg' }],
        app: { get: () => ioStub },
      };
      const res = makeRes();

      await createMessage(req, res);

      assert.equal(res.statusCode, 201, 'should return 201 for image-only');
      assert.ok(res.body.message, 'should return message object');
      assert.equal(res.body.message.mediaUrls.length, 1, 'mediaUrls should have 1 entry');
      assert.equal(res.body.message.mediaUrls[0], 'https://s3/messages/x.jpg');
    });

    it('neither text nor files → 400', async () => {
      const { sender, conversation } = await seedConversation();

      const req = {
        user: { id: sender._id.toString() },
        params: { id: conversation._id.toString() },
        body: {},
        files: [],
        app: { get: () => makeIoStub() },
      };
      const res = makeRes();

      await createMessage(req, res);

      assert.equal(res.statusCode, 400, 'should return 400');
      assert.equal(res.body.error, 'text or media is required');
    });

    it('disallowed mimetype → 400', async () => {
      const { sender, conversation } = await seedConversation();

      const req = {
        user: { id: sender._id.toString() },
        params: { id: conversation._id.toString() },
        body: {},
        files: [{ buffer: Buffer.from('fake'), mimetype: 'application/pdf' }],
        app: { get: () => makeIoStub() },
      };
      const res = makeRes();

      await createMessage(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 for disallowed mimetype');
      assert.equal(res.body.error, 'Invalid file type');
    });

    it('non-member sender → 403', async () => {
      const { outsider, conversation } = await seedConversation();

      const req = {
        user: { id: outsider._id.toString() },
        params: { id: conversation._id.toString() },
        body: { text: 'Trying to intrude' },
        files: [],
        app: { get: () => makeIoStub() },
      };
      const res = makeRes();

      await createMessage(req, res);

      assert.equal(res.statusCode, 403, 'should return 403 for non-member');
      assert.equal(res.body.error, 'Forbidden');
    });

    it('unknown conversation → 404', async () => {
      const { sender } = await seedConversation();
      const fakeId = new mongoose.Types.ObjectId().toString();

      const req = {
        user: { id: sender._id.toString() },
        params: { id: fakeId },
        body: { text: 'Hello' },
        files: [],
        app: { get: () => makeIoStub() },
      };
      const res = makeRes();

      await createMessage(req, res);

      assert.equal(res.statusCode, 404, 'should return 404 for unknown conversation');
      assert.equal(res.body.error, 'Conversation not found');
    });

  });

  describe('listMessages()', () => {

    it('returns messages reverse-chronological with hasMore false', async () => {
      const { sender, recipient, conversation } = await seedConversation();
      const Message = require('../models/Message');

      // Seed two messages
      await Message.create({ conversationId: conversation._id, senderId: sender._id, text: 'First' });
      await Message.create({ conversationId: conversation._id, senderId: recipient._id, text: 'Second' });

      const req = {
        user: { id: sender._id.toString() },
        params: { id: conversation._id.toString() },
        query: {},
      };
      const res = makeRes();

      await listMessages(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.ok(Array.isArray(res.body.messages), 'should return messages array');
      assert.ok(res.body.messages.length >= 2, 'should have at least 2 messages');
      assert.equal(res.body.hasMore, false, 'hasMore should be false');

      // Reverse chronological: most recent first (Second before First)
      const first = res.body.messages[0];
      const second = res.body.messages[1];
      assert.ok(
        first._id.toString() >= second._id.toString(),
        'messages should be reverse-chronological (newer _id first)'
      );
    });

    it('non-member caller → 403', async () => {
      const { outsider, conversation } = await seedConversation();

      const req = {
        user: { id: outsider._id.toString() },
        params: { id: conversation._id.toString() },
        query: {},
      };
      const res = makeRes();

      await listMessages(req, res);

      assert.equal(res.statusCode, 403, 'should return 403 for non-member list');
    });

    it('unknown conversation → 404', async () => {
      const { sender } = await seedConversation();
      const fakeId = new mongoose.Types.ObjectId().toString();

      const req = {
        user: { id: sender._id.toString() },
        params: { id: fakeId },
        query: {},
      };
      const res = makeRes();

      await listMessages(req, res);

      assert.equal(res.statusCode, 404, 'should return 404 for unknown conversation');
    });

  });

});
