'use strict';

const { Router } = require('express');
const { presign } = require('../controllers/media.controller');

const router = Router();

// No auth — presign happens during registration before the user account exists;
// content-type + size validation is the guard against misuse.
router.post('/presign', presign);

module.exports = router;
