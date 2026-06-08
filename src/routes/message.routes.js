'use strict';

const { Router } = require('express');
const multer = require('multer');
const authenticate = require('../middlewares/auth.middleware');
const { createMessage, listMessages } = require('../controllers/message.controller');
const { MAX_UPLOAD_BYTES } = require('../config/env');

const router = Router();

// multer with memory storage — images are streamed straight to S3 (T-06-08)
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

router.post('/:id/messages', authenticate, uploadImages, createMessage);
router.get('/:id/messages', authenticate, listMessages);

module.exports = router;
