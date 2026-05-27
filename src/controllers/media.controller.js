'use strict';

const crypto = require('crypto');
const { getPresignedPutUrl } = require('../services/s3.service');
const { MAX_UPLOAD_BYTES } = require('../config/env');

const ALLOWED_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

const EXTENSION_MAP = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
};

/**
 * POST /api/media/presign
 * Validates content-type + size, then issues a presigned S3 PUT URL.
 * Server generates the S3 key — client never controls key path (T-02-08b).
 */
async function presign(req, res) {
  const { contentType, size } = req.body || {};

  if (!contentType || size === undefined || size === null) {
    return res.status(400).json({ error: 'contentType and size are required' });
  }

  if (!ALLOWED_CONTENT_TYPES.has(contentType)) {
    return res.status(400).json({ error: 'contentType must be image/jpeg, image/png, or image/webp' });
  }

  if (size > MAX_UPLOAD_BYTES) {
    return res.status(400).json({ error: `File size exceeds the ${MAX_UPLOAD_BYTES} byte limit` });
  }

  try {
    const ext = EXTENSION_MAP[contentType];
    const key = `avatars/${crypto.randomUUID()}.${ext}`;

    const { uploadUrl, objectUrl } = await getPresignedPutUrl({ key, contentType });

    return res.status(200).json({ uploadUrl, key, objectUrl });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { presign };
