'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { listNotifications, markRead, deleteNotification } = require('../controllers/notification.controller');

const router = Router();

// GET /api/notifications — authenticated user lists their own notifications
router.get('/', authenticate, listNotifications);

// PATCH /api/notifications/:id/read — authenticated owner marks a notification as read
router.patch('/:id/read', authenticate, markRead);

// DELETE /api/notifications/:id — authenticated owner deletes a notification
router.delete('/:id', authenticate, deleteNotification);

module.exports = router;
