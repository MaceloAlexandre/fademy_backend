'use strict';

const { Router } = require('express');
const multer = require('multer');
const authenticate = require('../middlewares/auth.middleware');
const { createPost, getFeed, getPost } = require('../controllers/post.controller');
const { MAX_UPLOAD_BYTES } = require('../config/env');

const router = Router();

// multer with memory storage — images are streamed straight to S3 (T-04-04)
const uploadMiddleware = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_UPLOAD_BYTES },
});

// Wraps multer.array to surface upload errors as 400 (not 500)
function uploadImages(req, res, next) {
  uploadMiddleware.array('images', 4)(req, res, (err) => {
    if (err) {
      return res.status(400).json({ error: 'File upload error' });
    }
    return next();
  });
}

// IMPORTANT: /feed MUST be declared before /:id — otherwise 'feed' is captured as an id param
router.get('/feed', authenticate, getFeed);

router.post('/', authenticate, uploadImages, createPost);

router.get('/:id', authenticate, getPost);

module.exports = router;
