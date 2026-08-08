const admin = require('firebase-admin');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');

if (process.env.NODE_ENV !== 'production') {
  // Use local emulators in development only when they are not already configured.
  process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';
  process.env.FIREBASE_AUTH_EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || '127.0.0.1:9099';
}

const projectId = process.env.FIREBASE_PROJECT_ID || 'burnouttracker-a3738a4f';

let app;
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
  app = admin.initializeApp({
    credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)),
    projectId
  });
} else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  app = admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId
  });
} else if (process.env.FIRESTORE_EMULATOR_HOST || process.env.FIREBASE_AUTH_EMULATOR_HOST) {
  app = admin.initializeApp({ projectId });
} else {
  try {
    app = admin.initializeApp({
      credential: admin.credential.applicationDefault(),
      projectId
    });
  } catch (error) {
    console.warn('Firebase Admin SDK could not use application default credentials. Falling back to project-only initialization.', error.message);
    app = admin.initializeApp({ projectId });
  }
}

const db = getFirestore(app);
const auth = getAuth(app);
module.exports = { db, auth, FieldValue };