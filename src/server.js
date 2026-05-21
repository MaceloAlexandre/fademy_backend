'use strict';

const env = require('./config/env');
const { connectDB } = require('./config/db');
const app = require('./app');

connectDB(env.MONGODB_URI)
  .then(() => {
    app.listen(env.PORT, () => {
      console.log(`Fademy server running on port ${env.PORT}`);
    });
  })
  .catch((err) => {
    console.error('Failed to connect to MongoDB:', err.message);
    process.exit(1);
  });
