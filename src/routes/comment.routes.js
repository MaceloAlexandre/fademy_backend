'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { createComment, listComments } = require('../controllers/comment.controller');

const router = Router();

// POST /api/posts/:postId/comments — create a comment on a post
router.post('/:postId/comments', authenticate, createComment);

// GET /api/posts/:postId/comments — list all comments on a post
router.get('/:postId/comments', authenticate, listComments);

module.exports = router;
