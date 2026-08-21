const admin = require('firebase-admin');
const { cert, applicationDefault } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');

if (process.env.NODE_ENV !== 'production') {
  process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';
  process.env.FIREBASE_AUTH_EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || '127.0.0.1:9099';
}

const projectId = process.env.FIREBASE_PROJECT_ID || 'burnouttracker-a3738a4f';

function initializeFirebaseApp() {
  if (admin.apps && admin.apps.length > 0) {
    return admin.apps[0];
  }

  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    try {
      return admin.initializeApp({
        credential: cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)),
        projectId
      });
    } catch (error) {
      console.warn('Failed to initialize Firebase with FIREBASE_SERVICE_ACCOUNT. Falling back to a safer initialization.', error.message);
    }
  }

  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    try {
      return admin.initializeApp({
        credential: applicationDefault(),
        projectId
      });
    } catch (error) {
      console.warn('Failed to initialize Firebase with application default credentials. Falling back to project-only initialization.', error.message);
    }
  }

  try {
    return admin.initializeApp({
      credential: applicationDefault(),
      projectId
    });
  } catch (error) {
    console.warn('Firebase Admin SDK could not use credentials. Falling back to project-only initialization.', error.message);
    return admin.initializeApp({ projectId });
  }
}

const app = initializeFirebaseApp();
const db = getFirestore(app);
const auth = getAuth(app);
module.exports = { db, auth, FieldValue };