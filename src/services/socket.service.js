'use strict';

const { Server } = require('socket.io');
const { verifyToken } = require('./token.service');
const env = require('../config/env');

let _io = null;

/**
 * Initialise Socket.io on the given HTTP server.
 * - Sets CORS on the Server constructor (not inherited from Express cors()).
 * - Uses io.use() for JWT handshake authentication (HS256-pinned via verifyToken).
 * - On connection, joins a per-user room keyed on socket.data.user.id (T-06-04).
 * @param {import('http').Server} httpServer
 * @returns {import('socket.io').Server}
 */
function initSocket(httpServer) {
  const io = new Server(httpServer, {
    cors: { origin: env.FRONTEND_ORIGIN, credentials: false },
  });

  // Handshake authentication middleware (Pattern 2 — mirrors auth.middleware.js verbatim shape)
  io.use((socket, next) => {
    const token = socket.handshake.auth && socket.handshake.auth.token;
    if (!token) {
      return next(new Error('Authentication required'));
    }
    try {
      const decoded = verifyToken(token);
      socket.data.user = {
        id: decoded.sub,
        role: decoded.role,
        institutionId: decoded.institutionId,
      };
      return next();
    } catch (_err) {
      return next(new Error('Invalid or expired token'));
    }
  });

  // Per-user room join on connection (T-06-04: guard against socket.join(undefined))
  io.on('connection', (socket) => {
    if (socket.data.user && socket.data.user.id) {
      socket.join(socket.data.user.id);
    }
  });

  _io = io;
  return io;
}

/**
 * Returns the singleton io instance (fallback for non-req contexts).
 * Prefer req.app.get('io') in controllers.
 * @returns {import('socket.io').Server|null}
 */
function getIO() {
  return _io;
}

module.exports = { initSocket, getIO };
