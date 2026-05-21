'use strict';

const { verifyToken } = require('../services/token.service');

/**
 * JWT authentication middleware.
 * Reads Bearer token from Authorization header, verifies it, and attaches
 * req.user = { id, role, institutionId } on success.
 * Responds 401 and returns (without calling next) on any auth failure.
 * Fails closed — any uncertainty results in 401, never pass-through.
 */
function authenticate(req, res, next) {
  const authHeader = req.headers && req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authentication required' });
  }

  const token = authHeader.slice(7); // remove 'Bearer '

  try {
    const decoded = verifyToken(token);
    req.user = {
      id: decoded.sub,
      role: decoded.role,
      institutionId: decoded.institutionId,
    };
    return next();
  } catch (_err) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}

module.exports = authenticate;
