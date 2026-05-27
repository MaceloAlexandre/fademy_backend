'use strict';

const crypto = require('crypto');
const Invite = require('../models/Invite');
const Institution = require('../models/Institution');

// Slot caps by roleTarget (D-04)
const SLOT_CAPS = {
  aluno: 50,
  professor: 10,
  funcionario: 10,
  coordenador: 10,
  admin: 10,
};

/**
 * POST /api/invites
 * Protected: authenticate + requireRole(non-aluno)
 * Creates a multi-use invite link scoped to the caller's institution.
 */
async function createInvite(req, res) {
  const { roleTarget, maxUses, expiresAt: expiresAtInput } = req.body || {};
  const { id: invitedById, role: callerRole, institutionId } = req.user;

  if (!roleTarget || maxUses == null) {
    return res.status(400).json({ error: 'roleTarget and maxUses are required' });
  }

  // D-07: only admin can generate an admin-target invite
  if (roleTarget === 'admin' && callerRole !== 'admin') {
    return res.status(403).json({ error: 'Only admin can generate admin invites' });
  }

  // D-04: enforce slot cap per roleTarget
  const cap = SLOT_CAPS[roleTarget];
  if (cap == null) {
    return res.status(400).json({ error: 'Invalid roleTarget' });
  }
  if (maxUses > cap) {
    return res.status(400).json({ error: `maxUses exceeds cap of ${cap} for roleTarget ${roleTarget}` });
  }

  try {
    const token = crypto.randomUUID();
    // D-03: default expiry 7 days, configurable
    const expiresAt = expiresAtInput
      ? new Date(expiresAtInput)
      : new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);

    const invite = await Invite.create({
      institutionId,
      invitedById,
      roleTarget,
      token,
      maxUses,
      usesCount: 0,
      status: 'active',
      expiresAt,
    });

    return res.status(201).json({
      token: invite.token,
      link: `/invite/${invite.token}`,
    });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * GET /api/invites/:token
 * Public — no auth required.
 * Returns invite details for the AcceptInvite screen.
 */
async function getInvite(req, res) {
  const { token } = req.params;

  try {
    const invite = await Invite.findOne({ token });
    if (!invite) {
      return res.status(404).json({ error: 'Invite not found' });
    }

    const institution = await Institution.findById(invite.institutionId).lean();

    const now = new Date();
    const closed =
      invite.status !== 'active' ||
      invite.expiresAt < now ||
      invite.usesCount >= invite.maxUses;

    let reason = null;
    if (invite.status === 'revoked') reason = 'revoked';
    else if (invite.expiresAt < now) reason = 'expired';
    else if (invite.usesCount >= invite.maxUses) reason = 'full';

    return res.status(200).json({
      roleTarget: invite.roleTarget,
      institutionName: institution ? institution.name : null,
      remainingSlots: invite.maxUses - invite.usesCount,
      valid: !closed,
      reason,
    });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * POST /api/invites/:token/revoke
 * Protected: authenticate + requireRole(non-aluno)
 * Revokes an active invite scoped to the caller's institution.
 */
async function revokeInvite(req, res) {
  const { token } = req.params;
  const { institutionId } = req.user;

  try {
    const invite = await Invite.findOneAndUpdate(
      { token, institutionId },
      { status: 'revoked' },
      { new: true }
    );

    if (!invite) {
      return res.status(404).json({ error: 'Invite not found' });
    }

    return res.status(200).json({ message: 'Invite revoked', token: invite.token });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { createInvite, getInvite, revokeInvite };
