'use strict';

const { createServer } = require('http');
const env = require('./config/env');
const { connectDB } = require('./config/db');
const app = require('./app');
const { initSocket } = require('./services/socket.service');

connectDB(env.MONGODB_URI)
  .then(() => {
    const httpServer = createServer(app);
    const io = initSocket(httpServer);
    app.set('io', io);
    httpServer.listen(env.PORT, () => {
      console.log(`Fademy server running on port ${env.PORT}`);
    });
  })
  .catch((err) => {
    console.error('Failed to connect to MongoDB:', err.message);
    process.exit(1);
  });
