'use strict';

const Conversation = require('../models/Conversation');
const Message = require('../models/Message');
const User = require('../models/User');
const Follow = require('../models/Follow');
const Block = require('../models/Block');

/**
 * POST /api/conversations
 * Authenticated. Find or create a 1:1 conversation, gated on mutual-follow + block + same-institution.
 */
async function createConversation(req, res) {
  const userId = req.user.id;
  const targetId = req.body.userId;

  if (userId === String(targetId)) {
    return res.status(400).json({ error: 'Cannot message yourself' });
  }

  try {
    const targetUser = await User.findById(targetId);
    if (!targetUser) {
      return res.status(404).json({ error: 'User not found' });
    }

    // T-06-06: Same-institution gate — explicit check (not deferred to runtime verification)
    if (String(targetUser.institutionId) !== String(req.user.institutionId)) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    // T-06-02: Bilateral block check (SOCL-05)
    const blocked = await Block.findOne({
      $or: [
        { blockerId: userId, blockedId: targetId },
        { blockerId: targetId, blockedId: userId },
      ],
    });
    if (blocked) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    // Bilateral mutual-follow check (both directions, status 'accepted')
    const [aFollowsB, bFollowsA] = await Promise.all([
      Follow.findOne({ followerId: userId, followingId: targetId, status: 'accepted' }),
      Follow.findOne({ followerId: targetId, followingId: userId, status: 'accepted' }),
    ]);
    if (!aFollowsB || !bFollowsA) {
      return res.status(403).json({ error: 'Both users must follow each other to start a chat' });
    }

    // Find-or-create (order-independent lookup via $all + $size)
    let conversation = await Conversation.findOne({
      isGroup: false,
      memberIds: { $all: [userId, targetId], $size: 2 },
    });

    if (!conversation) {
      conversation = await Conversation.create({
        memberIds: [userId, targetId],
        isGroup: false,
        createdById: userId,
      });
      return res.status(201).json({ conversation });
    }

    return res.status(200).json({ conversation });
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Conflict' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * GET /api/conversations
 * Authenticated. List caller's 1:1 conversations sorted by updatedAt desc, each with lastMessage preview.
 */
async function listConversations(req, res) {
  try {
    const conversations = await Conversation.find({
      memberIds: req.user.id,
      isGroup: false,
    })
      .sort({ updatedAt: -1 })
      .populate('memberIds', 'name avatarUrl')
      .lean();

    // Option A bounded N+1: fetch last message per conversation
    const withLastMessage = await Promise.all(
      conversations.map(async (c) => {
        const lastMessage = await Message.findOne({ conversationId: c._id })
          .sort({ _id: -1 })
          .lean();
        return { ...c, lastMessage: lastMessage || null };
      })
    );

    return res.status(200).json({ conversations: withLastMessage });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * GET /api/conversations/eligible-contacts
 * Authenticated. Returns users who mutually follow the caller (both directions accepted),
 * for use as the new-conversation picker data source.
 */
async function listEligibleContacts(req, res) {
  const userId = req.user.id;

  try {
    // Users the caller follows (accepted)
    const callerFollowing = await Follow.find({ followerId: userId, status: 'accepted' })
      .select('followingId')
      .lean();
    const followingIds = callerFollowing.map((f) => f.followingId.toString());

    // Among those, users who also follow the caller back (accepted)
    const mutualFollows = await Follow.find({
      followerId: { $in: followingIds },
      followingId: userId,
      status: 'accepted',
    })
      .select('followerId')
      .lean();

    const mutualIds = mutualFollows.map((f) => f.followerId);

    const users = await User.find({ _id: { $in: mutualIds } })
      .select('name avatarUrl role')
      .lean();

    return res.status(200).json({ users });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { createConversation, listConversations, listEligibleContacts };
