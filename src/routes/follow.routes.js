'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { acceptFollow, rejectFollow } = require('../controllers/follow.controller');

const router = Router();

// PATCH /api/follows/:id/accept — authenticated follow target accepts a pending request
router.patch('/:id/accept', authenticate, acceptFollow);

// PATCH /api/follows/:id/reject — authenticated follow target rejects a pending request
router.patch('/:id/reject', authenticate, rejectFollow);

module.exports = router;
