'use strict';

const mongoose = require('mongoose');

const categorySchema = new mongoose.Schema({
  name: { type: String, required: true },
  slug: { type: String, required: true, unique: true },
  type: { type: String, required: true, enum: ['content', 'area'] },
});

module.exports = mongoose.model('Category', categorySchema);
