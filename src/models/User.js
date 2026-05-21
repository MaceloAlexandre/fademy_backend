'use strict';

const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  institutionId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Institution',
    required: true,
  },
  name: { type: String, required: true },
  email: { type: String, required: true, unique: true },
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

module.exports = mongoose.model('User', userSchema);
