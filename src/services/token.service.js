'use strict';

const jwt = require('jsonwebtoken');
const { JWT_SECRET, JWT_EXPIRES_IN } = require('../config/env');

const ALGORITHM = 'HS256';

/**
 * Signs a JWT token.
 * @param {{ sub: string, role: string, institutionId: string }} payload
 * @returns {string} signed JWT
 */
function signToken(payload) {
  return jwt.sign(payload, JWT_SECRET, {
    algorithm: ALGORITHM,
    expiresIn: JWT_EXPIRES_IN,
  });
}

/**
 * Verifies a JWT token. Pins algorithm to HS256 to prevent alg-confusion attacks.
 * @param {string} token
 * @returns {object} decoded payload
 * @throws {Error} if token is invalid, expired, or uses wrong algorithm
 */
function verifyToken(token) {
  return jwt.verify(token, JWT_SECRET, { algorithms: [ALGORITHM] });
}

module.exports = { signToken, verifyToken };
