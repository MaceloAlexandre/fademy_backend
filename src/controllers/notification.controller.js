'use strict';

const Notification = require('../models/Notification');
const Follow = require('../models/Follow');
const User = require('../models/User');

/**
 * GET /api/notifications
 * Authenticated. Returns cursor-paginated notifications for the caller, most-recent-first,
 * with the caller's unread count. Notification has NO institutionId — scope by userId ONLY.
 * limit capped at 50 to prevent DoS (T-05-04).
 */
async function listNotifications(req, res) {
  try {
    const query = { userId: req.user.id };

    // Cursor-based pagination: _id < cursor (T-05-04)
    if (req.query.cursor) {
      query._id = { $lt: req.query.cursor };
    }

    // Cap limit to 50 to prevent DoS (T-05-04)
    const limit = Math.min(parseInt(req.query.limit, 10) || 20, 50);

    // Fetch one extra to detect hasMore
    const docs = await Notification.find(query)
      .sort({ _id: -1 })
      .limit(limit + 1)
      .populate('actorId', 'name avatarUrl');

    const hasMore = docs.length > limit;
    const notifications = docs.slice(0, limit);
    const nextCursor = hasMore ? notifications[notifications.length - 1]._id : null;

    // For follow_request notifications, attach the current Follow status
    const followRequestNotifs = notifications.filter(n => n.type === 'follow_request');
    if (followRequestNotifs.length > 0) {
      const followIds = followRequestNotifs.map(n => n.refId).filter(Boolean);
      const follows = await Follow.find({ _id: { $in: followIds } }).select('status').lean();
      const followMap = new Map(follows.map(f => [String(f._id), f.status]));
      followRequestNotifs.forEach(n => {
        n._doc.followStatus = followMap.get(String(n.refId)) ?? null;
      });
    }

    const unreadCount = await Notification.countDocuments({ userId: req.user.id, read: false });

    return res.status(200).json({ notifications, hasMore, nextCursor, unreadCount });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * PATCH /api/notifications/:id/read
 * Authenticated. Marks a single notification as read. Only the recipient may mark
 * their own notification as read (T-05-02 — IDOR guard).
 */
async function markRead(req, res) {
  try {
    const notification = await Notification.findById(req.params.id);
    if (!notification) {
      return res.status(404).json({ error: 'Notification not found' });
    }

    if (notification.userId.toString() !== req.user.id) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    notification.read = true;
    await notification.save();

    return res.status(200).json({ read: true });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function deleteNotification(req, res) {
  try {
    const notification = await Notification.findById(req.params.id);
    if (!notification) return res.status(404).json({ error: 'Notification not found' });
    if (notification.userId.toString() !== req.user.id) return res.status(403).json({ error: 'Forbidden' });
    await notification.deleteOne();
    return res.status(200).json({ deleted: true });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { listNotifications, markRead, deleteNotification };
