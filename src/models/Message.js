'use strict';

const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema({
  conversationId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Conversation',
    required: true,
  },
  senderId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  text: {
    type: String,
  },
  mediaUrls: [{ type: String }],
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

messageSchema.index({ conversationId: 1, _id: -1 });

module.exports = mongoose.model('Message', messageSchema);
