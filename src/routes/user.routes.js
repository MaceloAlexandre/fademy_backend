'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { follow, unfollow } = require('../controllers/follow.controller');
const { block } = require('../controllers/block.controller');

const router = Router();

// Social graph routes (Plan 03-01)
// POST   /api/users/:id/follow  — authenticated user follows another
router.post('/:id/follow', authenticate, follow);

// DELETE /api/users/:id/follow  — authenticated user unfollows another
router.delete('/:id/follow', authenticate, unfollow);

// POST   /api/users/:id/block   — authenticated user blocks another
router.post('/:id/block', authenticate, block);

// Profile and search routes will be added here by Plan 03-02

module.exports = router;
