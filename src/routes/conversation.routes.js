'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { createConversation, listConversations, listEligibleContacts } = require('../controllers/conversation.controller');

const router = Router();

// IMPORTANT: specific paths must be declared before /:id — prevent 'eligible-contacts' being captured as a param
router.get('/eligible-contacts', authenticate, listEligibleContacts);
router.post('/', authenticate, createConversation);
router.get('/', authenticate, listConversations);

module.exports = router;
