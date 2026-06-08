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

// Social graph routers (Plan 03)
app.use('/api/users', require('./routes/user.routes'));
app.use('/api/follows', require('./routes/follow.routes'));

// Posts, Comments & Feed (Plan 04)
app.use('/api/posts', require('./routes/post.routes'));
app.use('/api/posts', require('./routes/comment.routes'));
app.use('/api/categories', require('./routes/category.routes'));

// Notifications (Plan 05)
app.use('/api/notifications', require('./routes/notification.routes'));

// Chat (Plan 06)
app.use('/api/conversations', require('./routes/conversation.routes'));

module.exports = app;
