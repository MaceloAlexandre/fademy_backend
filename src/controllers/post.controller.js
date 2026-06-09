'use strict';

const Post = require('../models/Post');
const Follow = require('../models/Follow');
const Block = require('../models/Block');
// Category must be required to register its mongoose model before populate calls
require('../models/Category');
const { uploadPostImage } = require('../services/s3.service');

const ALLOWED_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

/**
 * POST /api/posts
 * Authenticated. Creates a post with optional images and link.
 * aluno role may NOT use linkUrl — returns 403 (CONT-03, T-04-01).
 * authorId and institutionId are taken from req.user (verified JWT), never from body (T-04-05).
 */
async function createPost(req, res) {
  const { id: authorId, role, institutionId } = req.user;
  const { text, linkUrl } = req.body;

  // Validate required text field
  if (!text || typeof text !== 'string' || !text.trim()) {
    return res.status(400).json({ error: 'text is required' });
  }

  // Role gate: aluno cannot post links (T-04-01)
  if (linkUrl && linkUrl.trim() && role === 'aluno') {
    return res.status(403).json({ error: 'Insufficient permissions' });
  }

  try {
    // Upload images to S3 (multer has already capped to 4 via array('images', 4))
    const imageUrls = [];
    for (const file of (req.files || [])) {
      if (!ALLOWED_CONTENT_TYPES.has(file.mimetype)) {
        return res.status(400).json({ error: 'Invalid file type' });
      }
      const { objectUrl } = await uploadPostImage({ buffer: file.buffer, contentType: file.mimetype });
      imageUrls.push(objectUrl);
    }

    // Normalize categoryIds: multipart may send a comma-separated string or array
    let categoryIds = req.body.categoryIds || [];
    if (typeof categoryIds === 'string') {
      categoryIds = categoryIds.split(',').map(id => id.trim()).filter(Boolean);
    }

    const hasLink = Boolean(linkUrl && linkUrl.trim());

    const post = await Post.create({
      authorId,
      institutionId,
      text: text.trim(),
      imageUrls,
      linkUrl: hasLink ? linkUrl.trim() : undefined,
      hasLink,
      categoryIds,
    });

    return res.status(201).json({ post });
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Duplicate entry' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * GET /api/posts/feed
 * Authenticated. Returns cursor-paginated feed of posts from accepted follows,
 * excluding posts from blocked users in either direction (FEED-01, FEED-02, FEED-03).
 * Scoped to req.user.institutionId (T-04-02).
 * limit capped at 50 to prevent DoS (T-04-06).
 */
async function getFeed(req, res) {
  const { id: userId, institutionId } = req.user;

  try {
    // Fetch accepted follow targets
    const follows = await Follow.find({ followerId: userId, status: 'accepted' });
    const followingIds = follows.map(f => f.followingId);

    // Fetch blocks in both directions
    const blocks = await Block.find({
      $or: [{ blockerId: userId }, { blockedId: userId }],
    });
    const blockedIds = blocks.map(b =>
      b.blockerId.toString() === userId.toString() ? b.blockedId : b.blockerId
    );

    // Build query — institutionId scoping prevents cross-tenant reads (T-04-02)
    const query = {
      authorId: { $in: followingIds, $nin: blockedIds },
      institutionId,
    };

    // Cursor-based pagination: _id < cursor (T-04-06)
    if (req.query.cursor) {
      query._id = { $lt: req.query.cursor };
    }

    // Cap limit to 50 to prevent DoS (T-04-06)
    const limit = Math.min(parseInt(req.query.limit, 10) || 20, 50);

    // Fetch one extra to detect hasMore
    const docs = await Post.find(query)
      .sort({ _id: -1 })
      .limit(limit + 1)
      .populate('authorId', 'name avatarUrl')
      .populate('categoryIds', 'name slug type');

    const hasMore = docs.length > limit;
    const posts = docs.slice(0, limit);
    const nextCursor = hasMore ? posts[posts.length - 1]._id : null;

    return res.status(200).json({ posts, hasMore, nextCursor });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * GET /api/posts/:id
 * Authenticated. Returns a single post scoped to the user's institution (T-04-02).
 * Returns 404 if not found or belongs to a different institution.
 */
async function getPost(req, res) {
  const { institutionId } = req.user;

  try {
    const post = await Post.findById(req.params.id)
      .populate('authorId', 'name avatarUrl')
      .populate('categoryIds', 'name slug type');

    if (!post || post.institutionId.toString() !== institutionId.toString()) {
      return res.status(404).json({ error: 'Post not found' });
    }

    return res.status(200).json({ post });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * GET /api/users/:id/posts
 * Authenticated. Returns posts by a specific user, scoped to caller's institution.
 * Supports cursor pagination. Respects blocks in either direction.
 */
async function getUserPosts(req, res) {
  const { id: callerId, institutionId } = req.user;
  const { Types } = require('mongoose');
  const targetId = req.params.id === 'me' ? callerId : req.params.id;

  if (!Types.ObjectId.isValid(targetId) && targetId !== callerId) {
    return res.status(400).json({ error: 'Invalid user id' });
  }

  try {
    // Block check — only block if THEY blocked ME (I blocked them → I can still view their profile/posts to unblock)
    if (targetId !== callerId) {
      const theyBlockedMe = await Block.findOne({ blockerId: targetId, blockedId: callerId });
      if (theyBlockedMe) return res.status(403).json({ error: 'Forbidden' });
    }

    const query = { authorId: targetId, institutionId };
    if (req.query.cursor) {
      if (!Types.ObjectId.isValid(req.query.cursor)) {
        return res.status(400).json({ error: 'Invalid cursor' });
      }
      query._id = { $lt: req.query.cursor };
    }

    const rawLimit = parseInt(req.query.limit, 10);
    const limit = Math.min(rawLimit >= 1 ? rawLimit : 20, 50);

    const docs = await Post.find(query)
      .sort({ _id: -1 })
      .limit(limit + 1)
      .populate('authorId', 'name avatarUrl')
      .populate('categoryIds', 'name slug type');

    const hasMore = docs.length > limit;
    const posts = docs.slice(0, limit);
    const nextCursor = hasMore ? posts[posts.length - 1]._id : null;

    return res.status(200).json({ posts, hasMore, nextCursor });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { createPost, getFeed, getPost, getUserPosts };
