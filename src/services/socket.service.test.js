'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';

const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { createServer } = require('http');
const { io: ioClient } = require('socket.io-client');
const { signToken } = require('./token.service');
const { initSocket } = require('./socket.service');

let httpServer;
let io;
let serverPort;

before(async () => {
  await new Promise((resolve) => {
    httpServer = createServer();
    io = initSocket(httpServer);
    httpServer.listen(0, () => {
      serverPort = httpServer.address().port;
      resolve();
    });
  });
});

after(async () => {
  await new Promise((resolve) => {
    io.close(() => {
      httpServer.close(resolve);
    });
  });
});

function makeClient(options = {}) {
  return ioClient(`http://localhost:${serverPort}`, {
    transports: ['websocket'],
    ...options,
  });
}

describe('socket.service', () => {
  describe('handshake authentication', () => {
    it('accepts connection with a valid JWT in auth.token', (_, done) => {
      const token = signToken({ sub: 'user-a-id', role: 'aluno', institutionId: 'inst-1' });
      const client = makeClient({ auth: { token } });

      client.on('connect', () => {
        assert.ok(client.connected, 'client should be connected');
        client.disconnect();
        done();
      });

      client.on('connect_error', (err) => {
        client.disconnect();
        done(new Error(`Expected connect but got connect_error: ${err.message}`));
      });
    });

    it('rejects connection with no token', (_, done) => {
      const client = makeClient({ auth: {} });

      client.on('connect_error', (err) => {
        assert.ok(err.message.includes('Authentication required') || err.message.length > 0, 'should get error message');
        client.disconnect();
        done();
      });

      client.on('connect', () => {
        client.disconnect();
        done(new Error('Expected connect_error but got connect'));
      });
    });

    it('rejects connection with an invalid/garbage token', (_, done) => {
      const client = makeClient({ auth: { token: 'garbage-token-abc' } });

      client.on('connect_error', (err) => {
        assert.ok(err.message.length > 0, 'should get error message for invalid token');
        client.disconnect();
        done();
      });

      client.on('connect', () => {
        client.disconnect();
        done(new Error('Expected connect_error but got connect'));
      });
    });
  });

  describe('per-user room delivery', () => {
    it('delivers message:new only to the targeted user room', (_, done) => {
      const tokenA = signToken({ sub: 'user-a-room', role: 'aluno', institutionId: 'inst-1' });
      const tokenB = signToken({ sub: 'user-b-room', role: 'aluno', institutionId: 'inst-1' });

      const clientA = makeClient({ auth: { token: tokenA } });
      const clientB = makeClient({ auth: { token: tokenB } });

      let aConnected = false;
      let bConnected = false;

      const tryEmit = () => {
        if (!aConnected || !bConnected) return;

        // Emit to user A's room only
        const payload = { text: 'hello A', conversationId: 'conv-1' };
        io.to('user-a-room').emit('message:new', payload);
      };

      let aReceived = false;
      let bReceived = false;

      clientA.on('connect', () => {
        aConnected = true;
        tryEmit();
      });

      clientB.on('connect', () => {
        bConnected = true;
        tryEmit();
      });

      clientA.on('message:new', (data) => {
        aReceived = true;
        // Give a short window to also detect if B incorrectly received it
        setTimeout(() => {
          assert.ok(aReceived, 'user A should receive the message');
          assert.equal(bReceived, false, 'user B should NOT receive the message targeted at A');
          clientA.disconnect();
          clientB.disconnect();
          done();
        }, 100);
      });

      clientB.on('message:new', () => {
        bReceived = true;
      });

      clientA.on('connect_error', (err) => {
        clientA.disconnect();
        clientB.disconnect();
        done(new Error(`clientA connect_error: ${err.message}`));
      });

      clientB.on('connect_error', (err) => {
        clientA.disconnect();
        clientB.disconnect();
        done(new Error(`clientB connect_error: ${err.message}`));
      });
    });
  });
});
