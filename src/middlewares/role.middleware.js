'use strict';

/**
 * Role-based authorization middleware factory.
 * Returns an Express middleware that:
 *   - Responds 401 if req.user is absent (auth middleware must run first)
 *   - Responds 403 if req.user.role is not in allowedRoles (rejects before controller)
 *   - Calls next() only when the role is explicitly allowed
 *
 * Always reject before reaching the controller — per AUTH-03 and CONTEXT.md.
 *
 * @param {...string} allowedRoles - roles permitted to access the route
 * @returns {Function} Express middleware
 */
function requireRole(...allowedRoles) {
  return function roleGuard(req, res, next) {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required' });
    }

    if (!req.user.role || !allowedRoles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Insufficient permissions' });
    }

    return next();
  };
}

module.exports = { requireRole };
