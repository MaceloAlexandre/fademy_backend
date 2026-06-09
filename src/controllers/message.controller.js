'use strict';

const Conversation = require('../models/Conversation');
const Message = require('../models/Message');
const notificationService = require('../services/notification.service');
const { uploadMessageImage } = require('../services/s3.service');

const ALLOWED_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

/**
 * POST /api/conversations/:id/messages
 * Authenticated. Member-gated. Sends a text and/or image message to a conversation.
 * senderId is taken from req.user.id only — never from the request body (T-06-07).
 * On success: persists Message, bumps conversation.updatedAt, dispatches message_received
 * notification with refId=conversationId (for Plan 04 deep-linking), emits message:new socket event.
 */
async function createMessage(req, res) {
  try {
    const senderId = req.user.id;
    const { text } = req.body;
    const conversationId = req.params.id;

    // Membership gate (T-06-07, T-06-11)
    const conversation = await Conversation.findById(conversationId);
    if (!conversation) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    if (!conversation.memberIds.some(m => m.toString() === senderId)) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    // Content guard: must have text or at least one file
    const hasText = typeof text === 'string' && text.trim();
    const files = req.files || [];
    if (!hasText && files.length === 0) {
      return res.status(400).json({ error: 'text or media is required' });
    }

    // Upload images to S3 — server owns the key (T-06-08)
    const mediaUrls = [];
    for (const file of files) {
      if (!ALLOWED_CONTENT_TYPES.has(file.mimetype)) {
        return res.status(400).json({ error: 'Invalid file type' });
      }
      const { objectUrl } = await uploadMessageImage({ buffer: file.buffer, contentType: file.mimetype });
      mediaUrls.push(objectUrl);
    }

    // Persist message
    const message = await Message.create({
      conversationId,
      senderId,
      text: hasText ? text.trim() : undefined,
      mediaUrls,
    });

    // Bump conversation activity
    conversation.updatedAt = new Date();
    await conversation.save();

    // Dispatch notification to the OTHER member
    // refId = conversationId (NOT message._id) so Plan 04 can deep-link straight to the thread
    // refModel = 'Conversation' so the notification carries the conversationId directly
    const recipientId = conversation.memberIds.find(m => m.toString() !== senderId);
    if (recipientId) {
      await notificationService.create(
        'message_received',
        senderId,
        recipientId,
        conversationId,
        'Conversation'
      );
    }

    // Emit live update to recipient's socket room (REST is the only write path — T-06-09)
    const io = req.app.get('io');
    if (io && recipientId) {
      io.to(recipientId.toString()).emit('message:new', message);
    }

    return res.status(201).json({ message });
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Duplicate entry' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * GET /api/conversations/:id/messages
 * Authenticated. Member-gated. Returns cursor-paginated messages reverse-chronological.
 * limit capped at 50 (T-06-11).
 */
async function listMessages(req, res) {
  try {
    const callerId = req.user.id;
    const conversationId = req.params.id;

    // Membership gate (T-06-11)
    const conversation = await Conversation.findById(conversationId);
    if (!conversation) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    if (!conversation.memberIds.some(m => m.toString() === callerId)) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const query = { conversationId };
    if (req.query.cursor) {
      query._id = { $lt: req.query.cursor };
    }

    const limit = Math.min(parseInt(req.query.limit, 10) || 20, 50);
    const docs = await Message.find(query).sort({ _id: -1 }).limit(limit + 1);

    const hasMore = docs.length > limit;
    const messages = docs.slice(0, limit);
    const nextCursor = hasMore ? messages[messages.length - 1]._id : null;

    return res.status(200).json({ messages, hasMore, nextCursor });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { createMessage, listMessages };
