'use strict';

const express = require('express');
const healthRouter = require('./routes/health.routes');

const app = express();

app.use(express.json());

// Health check — unauthenticated public probe
app.use(healthRouter);

// Auth + protected routers (Plan 02)
app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/admin', require('./routes/admin.routes'));

module.exports = app;
