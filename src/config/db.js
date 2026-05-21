'use strict';

const mongoose = require('mongoose');

/**
 * Connect to MongoDB.
 * @param {string} uri - MongoDB connection URI
 * @returns {Promise<typeof mongoose>} the mongoose instance after connecting
 */
async function connectDB(uri) {
  await mongoose.connect(uri);
  return mongoose;
}

/**
 * Returns the current Mongoose connection readyState.
 * 0 = disconnected, 1 = connected, 2 = connecting, 3 = disconnecting
 */
function getReadyState() {
  return mongoose.connection.readyState;
}

module.exports = { connectDB, getReadyState };
