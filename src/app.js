'use strict';

const express = require('express');
const cors = require('cors');
const env = require('./config/env');
const healthRouter = require('./routes/health.routes');

const app = express();

app.use(express.json());

// CORS — allow the React frontend origin (configurable via FRONTEND_ORIGIN env var)
app.use(cors({ origin: env.FRONTEND_ORIGIN, credentials: false }));

// Health check — unauthenticated public probe
app.use(healthRouter);

// Auth + protected routers (Plan 02)
app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/admin', require('./routes/admin.routes'));
app.use('/api/invites', require('./routes/invite.routes'));
app.use('/api/media', require('./routes/media.routes'));

module.exports = app;
