'use strict';

const mongoose = require('mongoose');

const postSchema = new mongoose.Schema({
  authorId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  institutionId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Institution',
    required: true,
  },
  text: {
    type: String,
    required: true,
    maxlength: 1000,
  },
  imageUrls: {
    type: [String],
    default: [],
  },
  linkUrl: {
    type: String,
  },
  hasLink: {
    type: Boolean,
    default: false,
  },
  categoryIds: {
    type: [mongoose.Schema.Types.ObjectId],
    ref: 'Category',
    default: [],
  },
  commentCount: {
    type: Number,
    default: 0,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

postSchema.index({ authorId: 1, createdAt: -1 });
postSchema.index({ institutionId: 1, createdAt: -1 });
// Supports cursor-based feed: _id < cursor, sorted _id desc
postSchema.index({ authorId: 1, _id: -1 });

module.exports = mongoose.model('Post', postSchema);
