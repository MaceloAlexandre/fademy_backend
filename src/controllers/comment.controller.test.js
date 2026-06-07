'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

// Spy on notificationService.create to track calls.
// Intercept BEFORE requiring the controller so the controller gets the spy object.
const notifSpy = { create: async () => {}, calls: 0 };
notifSpy.create = async function () { notifSpy.calls += 1; };

const Module = require('module');
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) {
    return notifSpy;
  }
  return originalLoad(request, parent, isMain);
};

// NOW require the controller (gets the spy)
const { createComment, listComments } = require('./comment.controller');

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
  const Post = require('../models/Post');
  const bcrypt = require('bcrypt');
  const { BCRYPT_ROUNDS } = require('../config/env');

  const inst = await Institution.create({
    name: 'Test Institution Comments',
    slug: 'test-inst-comments-' + Date.now(),
  });

  const otherInst = await Institution.create({
    name: 'Other Institution',
    slug: 'other-inst-comments-' + Date.now(),
  });

  const passwordHash = await bcrypt.hash('password123', BCRYPT_ROUNDS);

  const author = await User.create({
    institutionId: inst._id,
    name: 'Post Author',
    email: 'post-author-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const commenter = await User.create({
    institutionId: inst._id,
    name: 'Commenter',
    email: 'commenter-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const otherUser = await User.create({
    institutionId: otherInst._id,
    name: 'Other User',
    email: 'other-user-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const post = await Post.create({
    authorId: author._id,
    institutionId: inst._id,
    text: 'Test post for comments',
  });

  return { inst, otherInst, author, commenter, otherUser, post };
}

describe('comment.controller', () => {

  describe('createComment()', () => {
    it('returns 201, creates comment, and increments commentCount to 1 (CONT-05)', async () => {
      const { inst, commenter, post } = await seedFixture();
      const Post = require('../models/Post');

      const req = {
        user: { id: commenter._id.toString(), institutionId: inst._id.toString() },
        params: { postId: post._id.toString() },
        body: { text: 'Great post!' },
      };
      const res = makeRes();

      await createComment(req, res);

      assert.equal(res.statusCode, 201, 'should return 201');
      assert.ok(res.body.comment, 'should return comment object');
      assert.equal(res.body.comment.text, 'Great post!');

      const updatedPost = await Post.findById(post._id);
      assert.equal(updatedPost.commentCount, 1, 'commentCount should be 1 after one comment');
    });

    it('returns 404 when post belongs to a different institution (T-04C-01)', async () => {
      const { otherInst, otherUser, post } = await seedFixture();

      const req = {
        user: { id: otherUser._id.toString(), institutionId: otherInst._id.toString() },
        params: { postId: post._id.toString() },
        body: { text: 'Trying to comment cross-institution' },
      };
      const res = makeRes();

      await createComment(req, res);

      assert.equal(res.statusCode, 404, 'should return 404 for cross-institution post');
      assert.equal(res.body.error, 'Post not found');
    });

    it('does NOT call notificationService.create on a self-comment (T-04C-04)', async () => {
      const { inst, author, post } = await seedFixture();

      // Reset spy counter
      notifSpy.calls = 0;

      const req = {
        user: { id: author._id.toString(), institutionId: inst._id.toString() },
        params: { postId: post._id.toString() },
        body: { text: 'Author commenting on own post' },
      };
      const res = makeRes();

      await createComment(req, res);

      assert.equal(res.statusCode, 201, 'self-comment should still succeed with 201');
      assert.equal(notifSpy.calls, 0, 'notification service should NOT be called on self-comment');
    });

    it('calls notificationService.create for a cross-author comment', async () => {
      const { inst, commenter, post } = await seedFixture();

      // Reset spy counter
      notifSpy.calls = 0;

      const req = {
        user: { id: commenter._id.toString(), institutionId: inst._id.toString() },
        params: { postId: post._id.toString() },
        body: { text: 'Triggering notification' },
      };
      const res = makeRes();

      await createComment(req, res);

      assert.equal(res.statusCode, 201, 'cross-author comment should return 201');
      assert.equal(notifSpy.calls, 1, 'notification service should be called once for cross-author comment');
    });

    it('returns 400 when text is missing', async () => {
      const { inst, commenter, post } = await seedFixture();

      const req = {
        user: { id: commenter._id.toString(), institutionId: inst._id.toString() },
        params: { postId: post._id.toString() },
        body: {},
      };
      const res = makeRes();

      await createComment(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 when text is missing');
      assert.equal(res.body.error, 'text is required');
    });
  });

  describe('listComments()', () => {
    it('returns comments oldest-first with authorId populated (CONT-05)', async () => {
      const { inst, commenter, author, post } = await seedFixture();

      // Create two comments in sequence
      const req1 = {
        user: { id: commenter._id.toString(), institutionId: inst._id.toString() },
        params: { postId: post._id.toString() },
        body: { text: 'First comment' },
      };
      await createComment(req1, makeRes());

      const req2 = {
        user: { id: author._id.toString(), institutionId: inst._id.toString() },
        params: { postId: post._id.toString() },
        body: { text: 'Second comment' },
      };
      await createComment(req2, makeRes());

      const listReq = {
        params: { postId: post._id.toString() },
      };
      const listRes = makeRes();

      await listComments(listReq, listRes);

      assert.equal(listRes.statusCode, 200, 'listComments should return 200');
      assert.ok(Array.isArray(listRes.body.comments), 'should return comments array');
      assert.ok(listRes.body.comments.length >= 2, 'should have at least 2 comments');

      // Oldest first
      const first = listRes.body.comments[0];
      const second = listRes.body.comments[1];
      assert.ok(
        new Date(first.createdAt) <= new Date(second.createdAt),
        'comments should be sorted oldest-first'
      );

      // authorId populated
      assert.ok(first.authorId && first.authorId.name, 'authorId should be populated with name');
    });
  });

});
