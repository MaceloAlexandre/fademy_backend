'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { createConversation, listConversations } = require('../controllers/conversation.controller');

const router = Router();

router.post('/', authenticate, createConversation);
router.get('/', authenticate, listConversations);

module.exports = router;
