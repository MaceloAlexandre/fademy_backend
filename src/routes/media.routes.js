'use strict';

const { Router } = require('express');
const multer = require('multer');
const { upload } = require('../controllers/media.controller');
const { MAX_UPLOAD_BYTES } = require('../config/env');

const router = Router();

// In-memory storage: avatars are small (<= MAX_UPLOAD_BYTES) and streamed straight to object storage.
const uploadMiddleware = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_UPLOAD_BYTES },
});

// No auth — upload happens during registration before the user account exists;
// content-type + size validation is the guard against misuse.
router.post('/upload', (req, res, next) => {
  uploadMiddleware.single('file')(req, res, (err) => {
    if (err) {
      // Includes multer LIMIT_FILE_SIZE — surface as a 400 rather than a 500.
      return res.status(400).json({ error: 'File upload error' });
    }
    return next();
  });
}, upload);

module.exports = router;
