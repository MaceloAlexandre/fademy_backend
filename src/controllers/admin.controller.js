'use strict';

const Institution = require('../models/Institution');
const User = require('../models/User');

/**
 * GET /admin/overview
 * Returns the caller's institution document plus its user count.
 * All queries are scoped to req.user.institutionId — PLAT-01 pattern (T-02-03).
 * Protected by auth.middleware + requireRole('admin') — never reached without valid admin token.
 */
async function getInstitutionOverview(req, res) {
  const { institutionId } = req.user;

  try {
    const institution = await Institution.findById(institutionId).lean();

    if (!institution) {
      return res.status(404).json({ error: 'Institution not found' });
    }

    const userCount = await User.countDocuments({ institutionId });

    return res.status(200).json({
      institution,
      userCount,
    });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { getInstitutionOverview };
