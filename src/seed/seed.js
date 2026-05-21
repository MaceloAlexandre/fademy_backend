'use strict';

const bcrypt = require('bcrypt');

// Slug helper: lowercase, strip accents, replace spaces with hyphens
function toSlug(str) {
  return str
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9-]/g, '');
}

const CONTENT_CATEGORIES = ['Projetos', 'Aulas', 'Dia a dia', 'Meme'];
const AREA_CATEGORIES = [
  'Ciência da Computação',
  'Direito',
  'Marketing',
  'Filosofia',
  'Educação Física',
  'NEPA',
];

async function runSeed() {
  // Lazy-load models so they pick up the active mongoose connection
  const Institution = require('../models/Institution');
  const Category = require('../models/Category');
  const User = require('../models/User');

  // Read env (may already be set by caller; use defaults if not)
  const adminEmail = process.env.BOOTSTRAP_ADMIN_EMAIL || 'admin@fademy.com';
  const adminPassword = process.env.BOOTSTRAP_ADMIN_PASSWORD || 'changeme';
  const adminName = process.env.BOOTSTRAP_ADMIN_NAME || 'Admin';
  const bcryptRounds = parseInt(process.env.BCRYPT_ROUNDS, 10) || 10;

  // 1. Upsert institution
  const institution = await Institution.findOneAndUpdate(
    { slug: 'catolica-pb' },
    {
      $set: {
        name: 'Faculdade Católica da Paraíba',
        active: true,
      },
      $setOnInsert: { createdAt: new Date() },
    },
    { upsert: true, new: true }
  );

  // 2. Upsert categories
  const contentOps = CONTENT_CATEGORIES.map((name) => ({
    updateOne: {
      filter: { slug: toSlug(name) },
      update: { $set: { name, type: 'content' } },
      upsert: true,
    },
  }));
  const areaOps = AREA_CATEGORIES.map((name) => ({
    updateOne: {
      filter: { slug: toSlug(name) },
      update: { $set: { name, type: 'area' } },
      upsert: true,
    },
  }));
  await Category.bulkWrite([...contentOps, ...areaOps]);

  // 3. Upsert bootstrap admin
  // Hash only on insert ($setOnInsert) so re-runs don't alter the existing hash
  const passwordHash = await bcrypt.hash(adminPassword, bcryptRounds);
  await User.findOneAndUpdate(
    { email: adminEmail },
    {
      $set: {
        name: adminName,
        role: 'admin',
        institutionId: institution._id,
        active: true,
      },
      $setOnInsert: {
        passwordHash,
        isPrivate: false,
        createdAt: new Date(),
      },
    },
    { upsert: true, new: true }
  );
}

// Run as script
if (require.main === module) {
  const { connectDB } = require('../config/db');
  // Load env only when running as a standalone script
  require('dotenv').config();

  const uri = process.env.MONGODB_URI;
  if (!uri) {
    console.error('Error: MONGODB_URI is required');
    process.exit(1);
  }

  connectDB(uri)
    .then(() => runSeed())
    .then(() => {
      console.log('Seed completed: institution + 10 categories + bootstrap admin');
      process.exit(0);
    })
    .catch((err) => {
      console.error('Seed failed:', err);
      process.exit(1);
    });
}

module.exports = { runSeed };
