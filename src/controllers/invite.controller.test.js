'use strict';

const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const mongoose = require('mongoose');

// Require the controller at the top so MODULE_NOT_FOUND is an unambiguous RED signal
// before the controller exists.
const { createInvite, getInvite, revokeInvite } = require('./invite.controller');

let mongoServer;
let testUri;

// --- mock req/res helpers (role.middleware.test.js pattern) ---

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
  process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
  process.env.BCRYPT_ROUNDS = '4';

  const { connectDB } = require('../config/db');
  await connectDB(testUri);
});

after(async () => {
  await mongoose.disconnect();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

// --- helpers to seed data ---

async function seedInstitutionAndProfessor() {
  const Institution = require('../models/Institution');
  const User = require('../models/User');
  const bcrypt = require('bcrypt');
  const { BCRYPT_ROUNDS } = require('../config/env');

  // Create institution
  const inst = await Institution.create({
    name: 'Test Institution',
    slug: 'test-inst-' + Date.now(),
  });

  // Create professor
  const prof = await User.create({
    institutionId: inst._id,
    name: 'Prof Test',
    email: 'prof-' + Date.now() + '@test.com',
    passwordHash: await bcrypt.hash('password123', BCRYPT_ROUNDS),
    role: 'professor',
  });

  return { inst, prof };
}

// --- tests ---

describe('invite.controller', () => {

  describe('createInvite', () => {
    it('creates an invite (professor caller, roleTarget aluno, maxUses 50) — returns token + link, persisted with correct defaults', async () => {
      const { inst, prof } = await seedInstitutionAndProfessor();

      const req = {
        user: { id: prof._id.toString(), role: 'professor', institutionId: inst._id.toString() },
        body: { roleTarget: 'aluno', maxUses: 50 },
      };
      const res = makeRes();

      await createInvite(req, res);

      assert.equal(res.statusCode, 201, 'should return 201');
      assert.ok(res.body.token, 'should return token');
      assert.ok(res.body.link, 'should return link');

      // Verify persistence
      const Invite = require('../models/Invite');
      const saved = await Invite.findOne({ token: res.body.token });
      assert.ok(saved, 'invite should be persisted');
      assert.equal(saved.usesCount, 0, 'usesCount should be 0');
      assert.equal(saved.status, 'active', 'status should be active');
      assert.equal(saved.institutionId.toString(), inst._id.toString(), 'institutionId should match caller');
      assert.equal(saved.invitedById.toString(), prof._id.toString(), 'invitedById should match caller');
      // expiresAt should be ~7 days out (within 1 minute tolerance)
      const sevenDays = 7 * 24 * 60 * 60 * 1000;
      const diff = saved.expiresAt.getTime() - Date.now();
      assert.ok(diff > sevenDays - 60000, 'expiresAt should be at least ~7 days out');
      assert.ok(diff < sevenDays + 60000, 'expiresAt should not be much more than 7 days out');
    });

    it('rejects maxUses above the aluno cap (50) with 400 — no invite persisted', async () => {
      const { inst, prof } = await seedInstitutionAndProfessor();

      const Invite = require('../models/Invite');
      const countBefore = await Invite.countDocuments();

      const req = {
        user: { id: prof._id.toString(), role: 'professor', institutionId: inst._id.toString() },
        body: { roleTarget: 'aluno', maxUses: 51 },
      };
      const res = makeRes();

      await createInvite(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 when maxUses exceeds cap');
      const countAfter = await Invite.countDocuments();
      assert.equal(countAfter, countBefore, 'no invite should be persisted');
    });

    it('rejects maxUses above the professor cap (10) with 400', async () => {
      const { inst, prof } = await seedInstitutionAndProfessor();

      const req = {
        user: { id: prof._id.toString(), role: 'professor', institutionId: inst._id.toString() },
        body: { roleTarget: 'professor', maxUses: 11 },
      };
      const res = makeRes();

      await createInvite(req, res);

      assert.equal(res.statusCode, 400, 'should return 400 when maxUses exceeds professor cap');
    });

    it('returns 403 when non-admin caller targets roleTarget admin (D-07)', async () => {
      const { inst, prof } = await seedInstitutionAndProfessor();

      const req = {
        user: { id: prof._id.toString(), role: 'professor', institutionId: inst._id.toString() },
        body: { roleTarget: 'admin', maxUses: 1 },
      };
      const res = makeRes();

      await createInvite(req, res);

      assert.equal(res.statusCode, 403, 'only admin can generate admin invites');
    });
  });

  describe('getInvite', () => {
    it('returns 200 with roleTarget, institution name, remainingSlots, valid:true for a valid token (no auth)', async () => {
      const { inst, prof } = await seedInstitutionAndProfessor();

      // Seed invite directly
      const Invite = require('../models/Invite');
      const crypto = require('crypto');
      const token = crypto.randomUUID();
      await Invite.create({
        institutionId: inst._id,
        invitedById: prof._id,
        roleTarget: 'aluno',
        token,
        maxUses: 10,
        usesCount: 3,
        status: 'active',
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      });

      const req = { params: { token } };
      const res = makeRes();

      await getInvite(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.equal(res.body.roleTarget, 'aluno');
      assert.ok(res.body.institutionName, 'should return institution name');
      assert.equal(res.body.remainingSlots, 7, 'remainingSlots = maxUses - usesCount');
      assert.equal(res.body.valid, true, 'should be valid');
    });

    it('returns 404 for unknown token', async () => {
      const req = { params: { token: 'unknown-token-' + Date.now() } };
      const res = makeRes();

      await getInvite(req, res);

      assert.equal(res.statusCode, 404);
    });

    it('returns valid:false for an expired invite', async () => {
      const { inst, prof } = await seedInstitutionAndProfessor();

      const Invite = require('../models/Invite');
      const crypto = require('crypto');
      const token = crypto.randomUUID();
      await Invite.create({
        institutionId: inst._id,
        invitedById: prof._id,
        roleTarget: 'aluno',
        token,
        maxUses: 10,
        usesCount: 0,
        status: 'active',
        expiresAt: new Date(Date.now() - 1000), // already expired
      });

      const req = { params: { token } };
      const res = makeRes();

      await getInvite(req, res);

      assert.equal(res.statusCode, 200, 'should still return 200');
      assert.equal(res.body.valid, false, 'expired invite should not be valid');
    });

    it('returns valid:false for a revoked invite', async () => {
      const { inst, prof } = await seedInstitutionAndProfessor();

      const Invite = require('../models/Invite');
      const crypto = require('crypto');
      const token = crypto.randomUUID();
      await Invite.create({
        institutionId: inst._id,
        invitedById: prof._id,
        roleTarget: 'aluno',
        token,
        maxUses: 10,
        usesCount: 0,
        status: 'revoked',
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      });

      const req = { params: { token } };
      const res = makeRes();

      await getInvite(req, res);

      assert.equal(res.statusCode, 200, 'should return 200');
      assert.equal(res.body.valid, false, 'revoked invite should not be valid');
    });
  });

  describe('revokeInvite', () => {
    it('revokes an active invite — status becomes revoked; subsequent getInvite reflects revoked', async () => {
      const { inst, prof } = await seedInstitutionAndProfessor();

      // Create invite via createInvite
      const reqCreate = {
        user: { id: prof._id.toString(), role: 'professor', institutionId: inst._id.toString() },
        body: { roleTarget: 'aluno', maxUses: 10 },
      };
      const resCreate = makeRes();
      await createInvite(reqCreate, resCreate);
      assert.equal(resCreate.statusCode, 201, 'create should succeed');

      const { token } = resCreate.body;

      // Revoke it
      const reqRevoke = {
        user: { id: prof._id.toString(), role: 'professor', institutionId: inst._id.toString() },
        params: { token },
      };
      const resRevoke = makeRes();
      await revokeInvite(reqRevoke, resRevoke);
      assert.equal(resRevoke.statusCode, 200, 'revoke should return 200');

      // Subsequent getInvite should show valid:false
      const reqGet = { params: { token } };
      const resGet = makeRes();
      await getInvite(reqGet, resGet);
      assert.equal(resGet.statusCode, 200);
      assert.equal(resGet.body.valid, false, 'revoked invite should be invalid');
    });
  });

});
