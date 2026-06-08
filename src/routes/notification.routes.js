'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { listNotifications, markRead } = require('../controllers/notification.controller');

const router = Router();

// GET /api/notifications — authenticated user lists their own notifications
router.get('/', authenticate, listNotifications);

// PATCH /api/notifications/:id/read — authenticated owner marks a notification as read
router.patch('/:id/read', authenticate, markRead);

module.exports = router;
