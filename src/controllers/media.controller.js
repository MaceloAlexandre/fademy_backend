'use strict';

const { uploadAvatar } = require('../services/s3.service');
const { MAX_UPLOAD_BYTES } = require('../config/env');

const ALLOWED_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

/**
 * POST /api/media/upload  (multipart/form-data, field "file")
 * Receives the avatar through the backend, validates content-type + size,
 * then streams it to object storage. Returns the server-owned key + public URL.
 */
async function upload(req, res) {
  const file = req.file;

  if (!file) {
    return res.status(400).json({ error: 'file is required' });
  }

  if (!ALLOWED_CONTENT_TYPES.has(file.mimetype)) {
    return res.status(400).json({ error: 'file must be image/jpeg, image/png, or image/webp' });
  }

  if (file.size > MAX_UPLOAD_BYTES) {
    return res.status(400).json({ error: `File size exceeds the ${MAX_UPLOAD_BYTES} byte limit` });
  }

  try {
    const { key, objectUrl } = await uploadAvatar({ buffer: file.buffer, contentType: file.mimetype });
    return res.status(201).json({ key, objectUrl });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { upload };
