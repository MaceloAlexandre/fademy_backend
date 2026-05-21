'use strict';

const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const mongoose = require('mongoose');

// These modules do not exist yet (RED phase) — test will fail on import
const { connectDB } = require('../config/db');
const { runSeed } = require('./seed');
const Institution = require('../models/Institution');
const User = require('../models/User');
const Category = require('../models/Category');

let mongoServer;
let testUri;

before(async () => {
  // Use mongodb-memory-server for a self-contained test DB
  let MongoMemoryServer;
  try {
    ({ MongoMemoryServer } = require('mongodb-memory-server'));
  } catch (err) {
    console.log('SKIP: mongodb-memory-server not available and no MONGODB_URI_TEST set');
    process.exit(0);
  }

  const uri = process.env.MONGODB_URI_TEST;
  if (uri) {
    testUri = uri;
  } else {
    mongoServer = await MongoMemoryServer.create();
    testUri = mongoServer.getUri();
  }

  // Set env vars needed by seed
  process.env.MONGODB_URI = testUri;
  process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
  process.env.BOOTSTRAP_ADMIN_EMAIL = 'admin@test.com';
  process.env.BOOTSTRAP_ADMIN_PASSWORD = 'PlainPassword123!';
  process.env.BOOTSTRAP_ADMIN_NAME = 'Test Admin';
  process.env.BCRYPT_ROUNDS = '4'; // fast for tests

  await connectDB(testUri);
});

after(async () => {
  await mongoose.disconnect();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

describe('Seed — end-to-end', () => {
  it('creates exactly 1 institution with slug catolica-pb', async () => {
    await runSeed();
    const count = await Institution.countDocuments({ slug: 'catolica-pb' });
    assert.equal(count, 1);
  });

  it('creates exactly 10 categories — 4 content, 6 area', async () => {
    const total = await Category.countDocuments();
    const content = await Category.countDocuments({ type: 'content' });
    const area = await Category.countDocuments({ type: 'area' });
    assert.equal(total, 10);
    assert.equal(content, 4);
    assert.equal(area, 6);
  });

  it('creates exactly 1 admin user linked to the catolica-pb institution', async () => {
    const institution = await Institution.findOne({ slug: 'catolica-pb' });
    assert.ok(institution, 'institution must exist');
    const adminCount = await User.countDocuments({ role: 'admin' });
    assert.equal(adminCount, 1);
    const admin = await User.findOne({ role: 'admin' });
    assert.ok(admin.institutionId.equals(institution._id), 'admin must be linked to the institution');
  });

  it('is idempotent — second run does not change counts', async () => {
    await runSeed();
    const institutions = await Institution.countDocuments();
    const categories = await Category.countDocuments();
    const admins = await User.countDocuments({ role: 'admin' });
    assert.equal(institutions, 1);
    assert.equal(categories, 10);
    assert.equal(admins, 1);
  });

  it('stores admin password as a bcrypt hash, not plaintext', async () => {
    const admin = await User.findOne({ role: 'admin' });
    assert.ok(admin, 'admin must exist');
    assert.notEqual(admin.passwordHash, 'PlainPassword123!');
    // bcrypt hashes always start with $2b$ or $2a$
    assert.match(admin.passwordHash, /^\$2[ab]\$/);
  });
});
