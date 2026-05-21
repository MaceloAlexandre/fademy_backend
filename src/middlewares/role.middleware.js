'use strict';

// Stub — will be implemented in GREEN phase
function requireRole(...allowedRoles) {
  return function (req, res, next) {
    return;
  };
}

module.exports = { requireRole };
