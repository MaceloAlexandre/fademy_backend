'use strict';

const mongoose = require('mongoose');

const inviteSchema = new mongoose.Schema({
  institutionId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Institution',
    required: true,
  },
  invitedById: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  roleTarget: {
    type: String,
    required: true,
    enum: ['admin', 'coordenador', 'professor', 'funcionario', 'aluno'],
  },
  token: {
    type: String,
    required: true,
    unique: true,
  },
  maxUses: {
    type: Number,
    required: true,
  },
  usesCount: {
    type: Number,
    default: 0,
  },
  status: {
    type: String,
    enum: ['active', 'revoked', 'expired'],
    default: 'active',
  },
  expiresAt: {
    type: Date,
    required: true,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

module.exports = mongoose.model('Invite', inviteSchema);
