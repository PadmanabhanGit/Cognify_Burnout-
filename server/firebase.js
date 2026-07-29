const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');
const path = require('path');

if (process.env.NODE_ENV !== 'production') {
  process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
  process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';
}

const app = initializeApp({
  credential: cert(path.resolve(__dirname, 'serviceAccountKey.json')),
});

const db = getFirestore(app);
const auth = getAuth(app);
const admin = require('firebase-admin');

module.exports = { db, auth, admin};