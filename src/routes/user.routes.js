'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { getProfile, getMe, updateMe, searchUsers, suggestUsers } = require('../controllers/user.controller');
const { getUserPosts } = require('../controllers/post.controller');
const { follow, unfollow } = require('../controllers/follow.controller');
const { block, unblock } = require('../controllers/block.controller');

const router = Router();

// Profile and search routes — MUST be registered before /:id (Pitfall 1: route ordering)
// GET /api/users/search?q=  — institution-scoped user search (PROF-03)
router.get('/search', authenticate, searchUsers);

// GET /api/users/suggestions  — institution-scoped "people you might want to follow" (FJC-04)
router.get('/suggestions', authenticate, suggestUsers);

// GET /api/users/me  — caller's own profile (PROF-02 isOwner path)
router.get('/me', authenticate, getMe);

// GET /api/users/:id  — view another user's profile (PROF-02)
router.get('/:id', authenticate, getProfile);

// PATCH /api/users/me  — toggle isPrivate (PROF-01)
router.patch('/me', authenticate, updateMe);

// Social graph routes (Plan 03-01)
// POST   /api/users/:id/follow  — authenticated user follows another
router.post('/:id/follow', authenticate, follow);

// DELETE /api/users/:id/follow  — authenticated user unfollows another
router.delete('/:id/follow', authenticate, unfollow);

// POST   /api/users/:id/block   — authenticated user blocks another
router.post('/:id/block', authenticate, block);

// DELETE /api/users/:id/block   — authenticated user unblocks another
router.delete('/:id/block', authenticate, unblock);

// GET    /api/users/:id/posts   — posts by a user, institution-scoped (PROF-02)
router.get('/:id/posts', authenticate, getUserPosts);

module.exports = router;
