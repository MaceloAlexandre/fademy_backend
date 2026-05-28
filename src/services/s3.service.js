'use strict';

const crypto = require('crypto');
const { S3Client, PutObjectCommand, HeadObjectCommand } = require('@aws-sdk/client-s3');
const { AWS_REGION, S3_BUCKET, S3_ACCESS_KEY_ID, S3_SECRET_ACCESS_KEY, S3_ENDPOINT, S3_PUBLIC_URL } = require('../config/env');

const s3 = new S3Client({
  region: AWS_REGION,
  credentials: {
    accessKeyId: S3_ACCESS_KEY_ID,
    secretAccessKey: S3_SECRET_ACCESS_KEY,
  },
  // S3-compatible providers (e.g. Contabo) need a custom endpoint + path-style addressing
  ...(S3_ENDPOINT ? { endpoint: S3_ENDPOINT, forcePathStyle: true } : {}),
});

const EXTENSION_MAP = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
};

// For Contabo the SDK bucket is the plain name (tenant is resolved from credentials),
// but the public read URL carries the tenant prefix — hence a separate S3_PUBLIC_URL base.
function buildObjectUrl(key) {
  if (S3_PUBLIC_URL) return `${S3_PUBLIC_URL}/${key}`;
  if (S3_ENDPOINT) return `${S3_ENDPOINT}/${S3_BUCKET}/${key}`;
  return `https://${S3_BUCKET}.s3.${AWS_REGION}.amazonaws.com/${key}`;
}

/**
 * Uploads an avatar buffer to object storage. The server owns the key — the client never controls it (T-02-08b).
 * @param {{ buffer: Buffer, contentType: string }} options
 * @returns {Promise<{ key: string, objectUrl: string }>}
 */
async function uploadAvatar({ buffer, contentType }) {
  const ext = EXTENSION_MAP[contentType] || '';
  const key = `avatars/${crypto.randomUUID()}${ext ? '.' + ext : ''}`;

  await s3.send(new PutObjectCommand({
    Bucket: S3_BUCKET,
    Key: key,
    Body: buffer,
    ContentType: contentType,
  }));

  return { key, objectUrl: buildObjectUrl(key) };
}

/**
 * Checks whether an object exists by issuing a HeadObject request.
 * @param {string} key
 * @returns {Promise<boolean>}
 */
async function objectExists(key) {
  try {
    await s3.send(new HeadObjectCommand({ Bucket: S3_BUCKET, Key: key }));
    return true;
  } catch (err) {
    // NotFound or NoSuchKey → object does not exist
    return false;
  }
}

module.exports = { uploadAvatar, objectExists };
