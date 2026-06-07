'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { listCategories } = require('../controllers/category.controller');

const router = Router();

router.get('/', authenticate, listCategories);

module.exports = router;
