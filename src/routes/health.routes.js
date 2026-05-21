'use strict';

const express = require('express');
const { getReadyState } = require('../config/db');

const router = express.Router();

router.get('/health', (req, res) => {
  const ready = getReadyState() === 1;
  if (ready) {
    return res.status(200).json({ status: 'ok', db: 'connected' });
  }
  return res.status(503).json({ status: 'degraded', db: 'disconnected' });
});

module.exports = router;
