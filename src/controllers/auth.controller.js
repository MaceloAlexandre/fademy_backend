'use strict';

const bcrypt = require('bcrypt');
const User = require('../models/User');
const { signToken } = require('../services/token.service');

/**
 * POST /auth/login
 * Accepts { email, password }, verifies credentials, returns { token }.
 * Generic 401 for both unknown email and wrong password — no user enumeration (T-02-04).
 * Only active users may log in.
 */
async function login(req, res) {
  const { email, password } = req.body || {};

  if (!email || !password) {
    return res.status(400).json({ error: 'email and password are required' });
  }

  try {
    const user = await User.findOne({ email }).lean();

    // Use a constant-time comparison stub to prevent timing-based enumeration:
    // if user not found, compare against a dummy hash so timing is similar.
    const DUMMY_HASH = '$2b$10$AAAAAAAAAAAAAAAAAAAAAAuBXVBrfNqcn8M.N0T1T4lhXCgG/2y4e';
    const hash = user ? user.passwordHash : DUMMY_HASH;
    const match = await bcrypt.compare(password, hash);

    if (!user || !user.active || !match) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const token = signToken({
      sub: user._id.toString(),
      role: user.role,
      institutionId: user.institutionId.toString(),
    });

    return res.status(200).json({ token });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { login };
