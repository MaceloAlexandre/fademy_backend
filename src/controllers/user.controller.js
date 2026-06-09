'use strict';

const User = require('../models/User');
const Follow = require('../models/Follow');
const Block = require('../models/Block');

async function getProfile(req, res) {
  const targetId = req.params.id;
  const { id: viewerId } = req.user;

  try {
    // If they blocked me → I cannot see their profile (404)
    const theyBlockedMe = await Block.findOne({ blockerId: targetId, blockedId: viewerId }).lean();
    if (theyBlockedMe) {
      return res.status(404).json({ error: 'User not found' });
    }

    // If I blocked them → I can still see their profile (to unblock), but flag it
    const iBlockedThem = await Block.findOne({ blockerId: viewerId, blockedId: targetId }).lean();

    const user = await User.findById(targetId).select('-passwordHash').lean();
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    const follow = await Follow.findOne({ followerId: viewerId, followingId: targetId }).lean();
    const followStatus = follow ? follow.status : null;

    const isOwner = viewerId === targetId;
    const canSeePrivate = isOwner || followStatus === 'accepted';

    if (user.isPrivate && !canSeePrivate) {
      return res.status(200).json({
        id: user._id,
        name: user.name,
        avatarUrl: user.avatarUrl,
        role: user.role,
        isPrivate: true,
        followStatus,
        isBlockedByMe: !!iBlockedThem,
      });
    }

    const [followersCount, followingCount] = await Promise.all([
      Follow.countDocuments({ followingId: targetId, status: 'accepted' }),
      Follow.countDocuments({ followerId: targetId, status: 'accepted' }),
    ]);

    return res.status(200).json({
      id: user._id,
      name: user.name,
      avatarUrl: user.avatarUrl,
      role: user.role,
      isPrivate: user.isPrivate,
      followStatus,
      followersCount,
      followingCount,
      isBlockedByMe: !!iBlockedThem,
    });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function getMe(req, res) {
  return getProfile({ ...req, params: { id: req.user.id } }, res);
}

async function updateMe(req, res) {
  const { isPrivate } = req.body;

  if (typeof isPrivate !== 'boolean') {
    return res.status(400).json({ error: 'isPrivate must be a boolean' });
  }

  try {
    const user = await User.findByIdAndUpdate(
      req.user.id,
      { isPrivate },
      { new: true }
    ).select('-passwordHash').lean();

    return res.status(200).json({
      id: user._id,
      name: user.name,
      avatarUrl: user.avatarUrl,
      role: user.role,
      isPrivate: user.isPrivate,
    });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function searchUsers(req, res) {
  const { q = '', page = 1, limit = 20 } = req.query;
  const { id: viewerId, institutionId } = req.user;

  if (!q || q.trim().length === 0) {
    return res.status(200).json({ users: [], total: 0, page: 1, limit: Number(limit) });
  }

  try {
    // ReDoS mitigation: truncate and escape metacharacters
    const safeQ = q.slice(0, 100).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(safeQ, 'i');

    // Build excluded IDs: all users with a block relationship to/from viewer + viewer themselves
    const blocks = await Block.find({
      $or: [{ blockerId: viewerId }, { blockedId: viewerId }],
    }).lean();

    const excludedIds = blocks.map((b) =>
      b.blockerId.toString() === viewerId ? b.blockedId : b.blockerId
    );
    excludedIds.push(viewerId);

    const skip = (Number(page) - 1) * Number(limit);

    const [users, total] = await Promise.all([
      User.find({ institutionId, name: regex, _id: { $nin: excludedIds } })
        .select('name avatarUrl role isPrivate')
        .skip(skip)
        .limit(Number(limit))
        .lean(),
      User.countDocuments({ institutionId, name: regex, _id: { $nin: excludedIds } }),
    ]);

    return res.status(200).json({ users, total, page: Number(page), limit: Number(limit) });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { getProfile, getMe, updateMe, searchUsers };
