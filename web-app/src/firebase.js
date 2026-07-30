import { initializeApp } from 'firebase/app';
import { getAuth, connectAuthEmulator } from 'firebase/auth';

// Using dummy config since Firebase is used in emulator mode typically for dev.
// The user should update this with real config if using production.
const firebaseConfig = {
  apiKey: "AIzaSyC9_G9adRw3eAuAtIShU9Pv58ffpxyh6fU",
  authDomain: "burnouttracker-a3738a4f.firebaseapp.com",
  projectId: "burnouttracker-a3738a4f",
  storageBucket: "burnouttracker-a3738a4f.firebasestorage.app",
  messagingSenderId: "966389564228",
  appId: "1:966389564228:android:ff2fcd66186641c1fc8d11"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);

// Connect to emulator if running locally
if (import.meta.env.DEV) {
  connectAuthEmulator(auth, 'http://127.0.0.1:9099');
}
