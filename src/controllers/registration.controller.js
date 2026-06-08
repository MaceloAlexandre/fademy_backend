'use strict';

const bcrypt = require('bcrypt');
const Invite = require('../models/Invite');
const User = require('../models/User');
const s3Service = require('../services/s3.service');
const notificationService = require('../services/notification.service');
const env = require('../config/env');

/**
 * POST /api/auth/register
 *
 * Body: { token, name, email, password, avatarUrl, avatarKey }
 *
 * Validates the invite (active, not expired, has slots), confirms the S3 object
 * exists via the server-issued avatarKey (T-02-08b — never reverse-parsed from URL),
 * creates the user scoped to the invite's institution and role, consumes a slot,
 * flips invite to expired on last slot. Returns 201 with user (no token — UI-SPEC
 * redirects to /login?registered=1).
 */
async function register(req, res) {
  const { token, name, email, password, avatarUrl, avatarKey } = req.body || {};

  // Validate required fields
  if (!token || !name || !email || !password || !avatarUrl || !avatarKey) {
    return res.status(400).json({ error: 'token, name, email, password, avatarUrl, and avatarKey are required' });
  }

  try {
    // Load invite by token
    const invite = await Invite.findOne({ token });

    // Reject: invite not found
    if (!invite) {
      return res.status(400).json({ error: 'Invalid invite token' });
    }

    // Reject: invite not active (revoked or already expired via status)
    if (invite.status !== 'active') {
      return res.status(400).json({ error: 'Invite is no longer valid' });
    }

    // Reject: invite expired by time
    if (invite.expiresAt < new Date()) {
      return res.status(400).json({ error: 'Invite has expired' });
    }

    // Reject: invite at capacity
    if (invite.usesCount >= invite.maxUses) {
      return res.status(400).json({ error: 'Invite has no remaining slots' });
    }

    // Confirm S3 object exists via server-issued avatarKey (D-10, T-02-08b)
    // Use avatarKey directly — never reverse-parse from client-supplied avatarUrl
    const exists = await s3Service.objectExists(avatarKey);
    if (!exists) {
      return res.status(400).json({ error: 'Profile photo not found in storage — please upload a photo first' });
    }

    // Hash the password (BCRYPT_ROUNDS from env, Phase 1 pattern)
    const passwordHash = await bcrypt.hash(password, env.BCRYPT_ROUNDS);

    // Create user scoped to invite's institution and role (T-02-09: never from req.body)
    let user;
    try {
      user = await User.create({
        institutionId: invite.institutionId,
        role: invite.roleTarget,
        name,
        email,
        passwordHash,
        avatarUrl,
      });
    } catch (err) {
      // Mongo duplicate-key error (compound index: institutionId + email)
      if (err.code === 11000) {
        return res.status(409).json({ error: 'Este e-mail já está em uso' });
      }
      throw err;
    }

    // Consume invite slot; flip to expired on last slot
    invite.usesCount += 1;
    if (invite.usesCount >= invite.maxUses) {
      invite.status = 'expired';
    }
    await invite.save();

    // Notify the invite creator that their invite was used (NOTF-03 — fire-and-forget, swallows its own errors)
    await notificationService.create('invite_received', user._id, invite.invitedById, user._id, 'User');

    // Return 201 with the created user — NO token (UI-SPEC redirects to /login?registered=1)
    return res.status(201).json({
      id: user._id.toString(),
      name: user.name,
      email: user.email,
      role: user.role,
      institutionId: user.institutionId.toString(),
      avatarUrl: user.avatarUrl,
    });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { register };
