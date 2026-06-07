'use strict';

const Category = require('../models/Category');

/**
 * GET /api/categories
 * Authenticated. Returns all seeded categories (global — no institutionId filter).
 */
async function listCategories(req, res) {
  try {
    const categories = await Category.find({});
    return res.status(200).json({ categories });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { listCategories };
