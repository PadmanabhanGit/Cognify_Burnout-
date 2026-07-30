const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');
const path = require('path');

if (process.env.NODE_ENV !== 'production') {
  // Use local emulators in development only
  process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
  process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';
}

let credential;
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
  // Production: Read from Environment Variable
  credential = cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT));
} else {
  // Local Development: Read from file
  credential = cert(path.resolve(__dirname, 'serviceAccountKey.json'));
}

const app = initializeApp({
  credential
});

const db = getFirestore(app);
const auth = getAuth(app);
module.exports = { db, auth, FieldValue };