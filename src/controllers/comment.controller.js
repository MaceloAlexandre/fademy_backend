'use strict';

const Comment = require('../models/Comment');
const Post = require('../models/Post');
const notificationService = require('../services/notification.service');

/**
 * POST /api/posts/:postId/comments
 * Authenticated. Create a comment on a post. Increments post.commentCount.
 * Fires post_comment notification unless commenter is the post author.
 */
async function createComment(req, res) {
  const { postId } = req.params;
  const { text } = req.body;
  const commenterId = req.user.id;
  const { institutionId } = req.user;

  if (!text || typeof text !== 'string' || text.trim() === '') {
    return res.status(400).json({ error: 'text is required' });
  }

  try {
    const post = await Post.findById(postId);
    if (!post || post.institutionId.toString() !== institutionId.toString()) {
      return res.status(404).json({ error: 'Post not found' });
    }

    const comment = await Comment.create({
      postId,
      authorId: commenterId,
      institutionId,
      text: text.trim(),
    });

    await Post.findByIdAndUpdate(postId, { $inc: { commentCount: 1 } });

    if (String(post.authorId) !== String(commenterId)) {
      await notificationService.create('post_comment', commenterId, post.authorId, post._id, 'Post');
    }

    return res.status(201).json({ comment });
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Duplicate entry' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
}

/**
 * GET /api/posts/:postId/comments
 * Authenticated. List all comments on a post, oldest first, with author info populated.
 */
async function listComments(req, res) {
  try {
    const comments = await Comment.find({ postId: req.params.postId })
      .sort({ createdAt: 1 })
      .populate('authorId', 'name avatarUrl');
    return res.status(200).json({ comments });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { createComment, listComments };
