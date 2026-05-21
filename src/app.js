'use strict';

const express = require('express');
const healthRouter = require('./routes/health.routes');

const app = express();

app.use(express.json());

// Health check — unauthenticated public probe
app.use(healthRouter);

// TODO(Plan 02): mount auth and protected routers here
// app.use('/api/auth', require('./routes/auth.routes'));
// app.use('/api/users', require('./routes/user.routes'));

module.exports = app;
