'use strict';

const { Router } = require('express');
const { login } = require('../controllers/auth.controller');
const { register } = require('../controllers/registration.controller');

const router = Router();

router.post('/login', login);
router.post('/register', register);

module.exports = router;
