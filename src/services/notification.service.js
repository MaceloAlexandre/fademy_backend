'use strict';

const Notification = require('../models/Notification');

async function create(type, actorId, userId, refId, refModel) {
  try {
    await Notification.create({ type, actorId, userId, refId, refModel });
  } catch (_err) {
    // fire-and-forget: swallow all errors silently
  }
}

module.exports = { create };
