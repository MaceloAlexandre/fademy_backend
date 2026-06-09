'use strict';

const Follow = require('../models/Follow');
const User = require('../models/User');
const Block = require('../models/Block');
const Notification = require('../models/Notification');
const notificationService = require('../services/notification.service');

/**
 * POST /api/users/:id/follow
 * Authenticated. Follow a user. Returns 201 with status 'accepted' or 'pending'.
 */
async function follow(req, res) {
  const followerId = req.user.id;
  const followingId = req.params.id;

  if (followerId === followingId) {
    return res.status(400).json({ error: 'Cannot follow yourself' });
  }

  try {
    const blocked = await Block.findOne({
      $or: [
        { blockerId: followerId, blockedId: followingId },
        { blockerId: followingId, blockedId: followerId },
      ],
    });
    if (blocked) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const target = await User.findById(followingId);
    if (!target) {
      return res.status(404).json({ error: 'User not found' });
    }

    const status = target.isPrivate ? 'pending' : 'accepted';
    const followDoc = await Follow.create({ followerId, followingId, status });

    // Remove any previous follow_request notification from this actor before creating new one
    if (status === 'pending') {
      await Notification.deleteOne({ type: 'follow_request', actorId: followerId, userId: followingId });
    }

    await notificationService.create(
      status === 'accepted' ? 'new_follower' : 'follow_request',
      followerId,
      followingId,
      followDoc._id,
      'Follow'
    );

    return res.status(201).json({ status: followDoc.status, followId: followDoc._id });
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Already following or request pending' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * DELETE /api/users/:id/follow
 * Authenticated. Unfollow a user. Idempotent — returns 200 even if no record existed.
 */
async function unfollow(req, res) {
  try {
    await Follow.deleteOne({ followerId: req.user.id, followingId: req.params.id });
    return res.status(200).json({ unfollowed: true });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * PATCH /api/follows/:id/accept
 * Authenticated. Accept a pending follow request. Only the follow target may accept.
 */
async function acceptFollow(req, res) {
  const followId = req.params.id;

  try {
    const followDoc = await Follow.findById(followId);
    if (!followDoc) {
      return res.status(404).json({ error: 'Follow request not found' });
    }

    if (followDoc.followingId.toString() !== req.user.id) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    if (followDoc.status !== 'pending') {
      return res.status(400).json({ error: 'No pending request' });
    }

    followDoc.status = 'accepted';
    await followDoc.save();

    await notificationService.create(
      'follow_accepted',
      followDoc.followingId,
      followDoc.followerId,
      followDoc._id,
      'Follow'
    );

    return res.status(200).json({ status: 'accepted' });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * PATCH /api/follows/:id/reject
 * Authenticated. Reject a pending follow request. Only the follow target may reject.
 */
async function rejectFollow(req, res) {
  const followId = req.params.id;

  try {
    const followDoc = await Follow.findById(followId);
    if (!followDoc) {
      return res.status(404).json({ error: 'Follow request not found' });
    }

    if (followDoc.followingId.toString() !== req.user.id) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    await Follow.findByIdAndDelete(followId);

    return res.status(200).json({ rejected: true });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { follow, unfollow, acceptFollow, rejectFollow };
