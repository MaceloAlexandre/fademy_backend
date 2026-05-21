'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { requireRole } = require('../middlewares/role.middleware');
const { getInstitutionOverview } = require('../controllers/admin.controller');

const router = Router();

// Protected: auth → role-gate (admin only) → controller
// Role rejection happens before the controller — AUTH-03
router.get('/overview', authenticate, requireRole('admin'), getInstitutionOverview);

module.exports = router;
