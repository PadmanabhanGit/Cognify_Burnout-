import { initializeApp } from 'firebase/app';
import { getAuth, connectAuthEmulator } from 'firebase/auth';

// Using dummy config since Firebase is used in emulator mode typically for dev.
// The user should update this with real config if using production.
const firebaseConfig = {
  apiKey: "dummy-api-key",
  authDomain: "dummy-auth-domain",
  projectId: "burnouttracker-a3738a4f", // Found in previous conversation logs
  storageBucket: "dummy-bucket",
  messagingSenderId: "dummy-sender",
  appId: "dummy-app-id"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);

// Connect to emulator if running locally
if (import.meta.env.DEV) {
  connectAuthEmulator(auth, 'http://127.0.0.1:9099');
}
