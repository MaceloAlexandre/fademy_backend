'use strict';

const Follow = require('../models/Follow');
const Block = require('../models/Block');
const Notification = require('../models/Notification');

/**
 * POST /api/users/:id/block
 * Authenticated. Block a user — removes all bilateral Follow docs and upserts a Block record.
 */
async function block(req, res) {
  const blockerId = req.user.id;
  const blockedId = req.params.id;

  if (blockerId === blockedId) {
    return res.status(400).json({ error: 'Cannot block yourself' });
  }

  try {
    await Follow.deleteMany({
      $or: [
        { followerId: blockerId, followingId: blockedId },
        { followerId: blockedId, followingId: blockerId },
      ],
    });

    // Clean up stale follow_request notifications in both directions
    await Notification.deleteMany({
      type: 'follow_request',
      $or: [
        { actorId: blockedId, userId: blockerId },
        { actorId: blockerId, userId: blockedId },
      ],
    });

    await Block.findOneAndUpdate(
      { blockerId, blockedId },
      { blockerId, blockedId },
      { upsert: true, new: true }
    );

    return res.status(200).json({ blocked: true });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function unblock(req, res) {
  const blockerId = req.user.id;
  const blockedId = req.params.id;

  if (blockerId === blockedId) {
    return res.status(400).json({ error: 'Cannot unblock yourself' });
  }

  try {
    await Block.findOneAndDelete({ blockerId, blockedId });
    return res.status(200).json({ unblocked: true });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { block, unblock };
