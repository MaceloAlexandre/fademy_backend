'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { requireRole } = require('../middlewares/role.middleware');
const { createInvite, getInvite, revokeInvite } = require('../controllers/invite.controller');

const router = Router();

// Protected: auth → role-gate (non-aluno) → createInvite
// requireRole gate enforces INVT-04 — aluno gets 403 before the controller
router.post('/', authenticate, requireRole('admin', 'coordenador', 'professor', 'funcionario'), createInvite);

// Public: no auth required
router.get('/:token', getInvite);

// Protected: auth → role-gate (non-aluno) → revokeInvite
router.post('/:token/revoke', authenticate, requireRole('admin', 'coordenador', 'professor', 'funcionario'), revokeInvite);

module.exports = router;
