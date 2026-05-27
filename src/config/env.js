'use strict';

require('dotenv').config();

const required = ['MONGODB_URI', 'JWT_SECRET'];

for (const key of required) {
  if (!process.env[key]) {
    throw new Error(`Missing required environment variable: ${key}`);
  }
}

module.exports = {
  MONGODB_URI: process.env.MONGODB_URI,
  PORT: parseInt(process.env.PORT, 10) || 3000,
  JWT_SECRET: process.env.JWT_SECRET,
  JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '7d',
  BOOTSTRAP_ADMIN_EMAIL: process.env.BOOTSTRAP_ADMIN_EMAIL || 'admin@fademy.com',
  BOOTSTRAP_ADMIN_PASSWORD: process.env.BOOTSTRAP_ADMIN_PASSWORD || 'changeme',
  BOOTSTRAP_ADMIN_NAME: process.env.BOOTSTRAP_ADMIN_NAME || 'Admin',
  BCRYPT_ROUNDS: parseInt(process.env.BCRYPT_ROUNDS, 10) || 10,
  // S3 presigned upload config (PLAT-03)
  AWS_REGION: process.env.AWS_REGION || 'us-east-1',
  S3_BUCKET: process.env.S3_BUCKET || 'fademy-dev',
  S3_ACCESS_KEY_ID: process.env.S3_ACCESS_KEY_ID || '',
  S3_SECRET_ACCESS_KEY: process.env.S3_SECRET_ACCESS_KEY || '',
  // Custom endpoint for S3-compatible providers (e.g. Contabo). Empty = real AWS S3.
  S3_ENDPOINT: process.env.S3_ENDPOINT || '',
  S3_PRESIGN_TTL: parseInt(process.env.S3_PRESIGN_TTL, 10) || 300,
  MAX_UPLOAD_BYTES: parseInt(process.env.MAX_UPLOAD_BYTES, 10) || 5 * 1024 * 1024,
  // CORS allowed origin for the React frontend
  FRONTEND_ORIGIN: process.env.FRONTEND_ORIGIN || 'http://localhost:5173',
};
