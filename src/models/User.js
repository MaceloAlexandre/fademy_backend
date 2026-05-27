'use strict';

const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  institutionId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Institution',
    required: true,
  },
  name: { type: String, required: true },
  email: { type: String, required: true },
  passwordHash: { type: String, required: true },
  avatarUrl: { type: String },
  role: {
    type: String,
    required: true,
    enum: ['admin', 'coordenador', 'professor', 'funcionario', 'aluno'],
  },
  isPrivate: { type: Boolean, default: false },
  active: { type: Boolean, default: true },
  createdAt: { type: Date, default: Date.now },
});

// Compound unique index: same email allowed in different institutions, but not twice in one (D-08)
userSchema.index({ institutionId: 1, email: 1 }, { unique: true });

module.exports = mongoose.model('User', userSchema);
