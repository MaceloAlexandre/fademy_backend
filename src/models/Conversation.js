'use strict';

const mongoose = require('mongoose');

const conversationSchema = new mongoose.Schema({
  memberIds: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
  ],
  name: {
    type: String,
  },
  isGroup: {
    type: Boolean,
    default: false,
  },
  isPublic: {
    type: Boolean,
    default: false,
  },
  createdById: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  updatedAt: {
    type: Date,
    default: Date.now,
  },
});

conversationSchema.index({ memberIds: 1, isGroup: 1 });

module.exports = mongoose.model('Conversation', conversationSchema);
