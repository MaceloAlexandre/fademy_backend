'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

// Mock notification.service BEFORE any module loads it
const Module = require('module');
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) {
    return { create: async () => {} };
  }
  return originalLoad(request, parent, isMain);
};

// Mock s3.service.uploadPostImage — no real S3 in tests
const fakeUploadPostImage = async ({ buffer, contentType }) => ({
  key: `posts/fake-key.jpg`,
  objectUrl: `https://s3.example.com/posts/fake-key.jpg`,
});

Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) {
    return { create: async () => {} };
  }
  if (request.includes('s3.service')) {
    return {
      uploadAvatar: async () => ({ key: 'avatars/fake', objectUrl: 'https://s3.example.com/avatars/fake' }),
      uploadPostImage: fakeUploadPostImage,
      objectExists: async () => true,
    };
  }
  return originalLoad(request, parent, isMain);
};

const { createPost, getFeed, getPost } = require('./post.controller');

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

async function seedData() {
  const Institution = require('../models/Institution');
  const User = require('../models/User');
  const bcrypt = require('bcrypt');
  const { BCRYPT_ROUNDS } = require('../config/env');

  const inst = await Institution.create({
    name: 'Test Institution',
    slug: 'test-inst-post-' + Date.now(),
  });

  const inst2 = await Institution.create({
    name: 'Other Institution',
    slug: 'other-inst-post-' + Date.now(),
  });

  const passwordHash = await bcrypt.hash('password123', BCRYPT_ROUNDS);

  const aluno = await User.create({
    institutionId: inst._id,
    name: 'Aluno User',
    email: 'aluno-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  const professor = await User.create({
    institutionId: inst._id,
    name: 'Professor User',
    email: 'prof-' + Date.now() + '@test.com',
    passwordHash,
    role: 'professor',
    isPrivate: false,
  });

  const otherUser = await User.create({
    institutionId: inst2._id,
    name: 'Other User',
    email: 'other-' + Date.now() + '@test.com',
    passwordHash,
    role: 'aluno',
    isPrivate: false,
  });

  return { inst, inst2, aluno, professor, otherUser };
}

describe('post.controller', () => {

  describe('createPost()', () => {
    it('returns 403 when aluno posts with linkUrl (CONT-03 — T-04-01)', async () => {
      const { inst, aluno } = await seedData();

      const req = {
        user: { id: aluno._id.toString(), role: 'aluno', institutionId: inst._id.toString() },
        body: { text: 'Hello world', linkUrl: 'https://example.com' },
        files: [],
      };
      const res = makeRes();

      await createPost(req, res);

      assert.equal(res.statusCode, 403, 'aluno with linkUrl should get 403');
      assert.equal(res.body.error, 'Insufficient permissions');
    });

    it('returns 201 with hasLink:true when professor posts with linkUrl (CONT-03)', async () => {
      const { inst, professor } = await seedData();

      const req = {
        user: { id: professor._id.toString(), role: 'professor', institutionId: inst._id.toString() },
        body: { text: 'Check this link', linkUrl: 'https://example.com' },
        files: [],
      };
      const res = makeRes();

      await createPost(req, res);

      assert.equal(res.statusCode, 201, 'professor with linkUrl should get 201');
      assert.equal(res.body.post.hasLink, true, 'hasLink should be true');
      assert.equal(res.body.post.linkUrl, 'https://example.com');
    });

    it('returns 400 when text is missing (CONT-01)', async () => {
      const { inst, aluno } = await seedData();

      const req = {
        user: { id: aluno._id.toString(), role: 'aluno', institutionId: inst._id.toString() },
        body: {},
        files: [],
      };
      const res = makeRes();

      await createPost(req, res);

      assert.equal(res.statusCode, 400, 'missing text should return 400');
      assert.equal(res.body.error, 'text is required');
    });

    it('returns 201 and populates imageUrls when files are attached (CONT-02)', async () => {
      const { inst, aluno } = await seedData();

      const req = {
        user: { id: aluno._id.toString(), role: 'aluno', institutionId: inst._id.toString() },
        body: { text: 'Post with image' },
        files: [
          { buffer: Buffer.from('fake-image'), mimetype: 'image/jpeg' },
        ],
      };
      const res = makeRes();

      await createPost(req, res);

      assert.equal(res.statusCode, 201);
      assert.equal(res.body.post.imageUrls.length, 1, 'should have one imageUrl');
      assert.ok(res.body.post.imageUrls[0].startsWith('https://'), 'imageUrl should be a URL');
    });
  });

  describe('getFeed()', () => {
    it('returns only posts from accepted-follow authors (FEED-01)', async () => {
      const { inst, aluno, professor } = await seedData();
      const Follow = require('../models/Follow');
      const Post = require('../models/Post');

      // aluno follows professor (accepted)
      await Follow.create({ followerId: aluno._id, followingId: professor._id, status: 'accepted' });

      // professor creates a post
      await Post.create({
        authorId: professor._id,
        institutionId: inst._id,
        text: 'Professor post',
      });

      const req = {
        user: { id: aluno._id.toString(), role: 'aluno', institutionId: inst._id.toString() },
        query: {},
      };
      const res = makeRes();

      await getFeed(req, res);

      assert.equal(res.statusCode, 200);
      assert.ok(Array.isArray(res.body.posts));
      assert.ok(res.body.posts.length >= 1, 'feed should contain professor post');
      assert.ok(
        res.body.posts.every(p => p.authorId._id.toString() === professor._id.toString()),
        'all posts should be from professor'
      );
    });

    it('excludes posts from blocked users in either direction (FEED-02 — T-04-03)', async () => {
      const { inst, aluno, professor } = await seedData();
      const Follow = require('../models/Follow');
      const Block = require('../models/Block');
      const Post = require('../models/Post');

      // Create another user to follow (not blocked)
      const User = require('../models/User');
      const bcrypt = require('bcrypt');
      const { BCRYPT_ROUNDS } = require('../config/env');
      const passwordHash = await bcrypt.hash('password123', BCRYPT_ROUNDS);

      const safeUser = await User.create({
        institutionId: inst._id,
        name: 'Safe User',
        email: 'safe-' + Date.now() + '@test.com',
        passwordHash,
        role: 'professor',
        isPrivate: false,
      });

      // aluno follows professor and safeUser (accepted)
      await Follow.create({ followerId: aluno._id, followingId: professor._id, status: 'accepted' });
      await Follow.create({ followerId: aluno._id, followingId: safeUser._id, status: 'accepted' });

      // aluno blocks professor
      await Block.create({ blockerId: aluno._id, blockedId: professor._id });

      // Both create posts
      await Post.create({ authorId: professor._id, institutionId: inst._id, text: 'Blocked author post' });
      await Post.create({ authorId: safeUser._id, institutionId: inst._id, text: 'Safe user post' });

      const req = {
        user: { id: aluno._id.toString(), role: 'aluno', institutionId: inst._id.toString() },
        query: {},
      };
      const res = makeRes();

      await getFeed(req, res);

      assert.equal(res.statusCode, 200);
      const authorIds = res.body.posts.map(p => p.authorId._id.toString());
      assert.ok(!authorIds.includes(professor._id.toString()), 'blocked author should be excluded');
      assert.ok(authorIds.includes(safeUser._id.toString()), 'safe user should be included');
    });

    it('excludes posts when the viewer is blocked by the author (FEED-02 reverse direction)', async () => {
      const { inst, aluno, professor } = await seedData();
      const Follow = require('../models/Follow');
      const Block = require('../models/Block');
      const Post = require('../models/Post');

      // aluno follows professor (accepted)
      await Follow.create({ followerId: aluno._id, followingId: professor._id, status: 'accepted' });

      // professor blocks aluno
      await Block.create({ blockerId: professor._id, blockedId: aluno._id });

      // professor creates a post
      await Post.create({ authorId: professor._id, institutionId: inst._id, text: 'Should not appear' });

      const req = {
        user: { id: aluno._id.toString(), role: 'aluno', institutionId: inst._id.toString() },
        query: {},
      };
      const res = makeRes();

      await getFeed(req, res);

      assert.equal(res.statusCode, 200);
      const authorIds = res.body.posts.map(p => p.authorId._id.toString());
      assert.ok(!authorIds.includes(professor._id.toString()), 'author who blocked viewer should be excluded');
    });

    it('returns hasMore:true and nextCursor when more posts than limit exist (FEED-03)', async () => {
      const { inst, aluno, professor } = await seedData();
      const Follow = require('../models/Follow');
      const Post = require('../models/Post');

      // aluno follows professor
      await Follow.create({ followerId: aluno._id, followingId: professor._id, status: 'accepted' });

      // Create 5 posts
      for (let i = 0; i < 5; i++) {
        await Post.create({ authorId: professor._id, institutionId: inst._id, text: `Post ${i}` });
      }

      const req = {
        user: { id: aluno._id.toString(), role: 'aluno', institutionId: inst._id.toString() },
        query: { limit: '3' },
      };
      const res = makeRes();

      await getFeed(req, res);

      assert.equal(res.statusCode, 200);
      assert.equal(res.body.posts.length, 3, 'should return exactly 3 posts');
      assert.equal(res.body.hasMore, true, 'hasMore should be true');
      assert.ok(res.body.nextCursor, 'nextCursor should be set');
    });
  });

  describe('getPost()', () => {
    it('returns 200 with post for matching institution', async () => {
      const { inst, aluno, professor } = await seedData();
      const Post = require('../models/Post');

      const post = await Post.create({ authorId: professor._id, institutionId: inst._id, text: 'Test post' });

      const req = {
        user: { id: aluno._id.toString(), role: 'aluno', institutionId: inst._id.toString() },
        params: { id: post._id.toString() },
      };
      const res = makeRes();

      await getPost(req, res);

      assert.equal(res.statusCode, 200);
      assert.ok(res.body.post, 'should return post');
      assert.equal(res.body.post._id.toString(), post._id.toString());
    });

    it('returns 404 for cross-institution post access (T-04-02)', async () => {
      const { inst, inst2, aluno, professor, otherUser } = await seedData();
      const Post = require('../models/Post');

      // professor from inst creates a post
      const post = await Post.create({ authorId: professor._id, institutionId: inst._id, text: 'Institution post' });

      // otherUser from inst2 tries to access it
      const req = {
        user: { id: otherUser._id.toString(), role: 'aluno', institutionId: inst2._id.toString() },
        params: { id: post._id.toString() },
      };
      const res = makeRes();

      await getPost(req, res);

      assert.equal(res.statusCode, 404, 'cross-institution post access should return 404');
      assert.equal(res.body.error, 'Post not found');
    });
  });

});
