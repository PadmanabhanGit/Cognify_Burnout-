#  Cognify Backend - Setup & Startup Guide

## Overview

The Cognify Backend is a **Node.js/Express application** that handles:
- User authentication (Login/Register with JWT)
- Burnout risk computation using ML algorithms
- Activity tracking (steps, calories, active minutes)
- Sleep and mood tracking
- Productivity monitoring
- Study time tracking
- Usage analytics
- Dashboard data aggregation

**Technology Stack:**
- Runtime: Node.js
- Framework: Express.js
- Database: SQLite 3
- Authentication: JWT (JSON Web Tokens)
- Password Hashing: bcryptjs

---

## Prerequisites

### 1. Install Node.js & npm

**Download from:** https://nodejs.org/

**Verify installation:**
```powershell
node --version
npm --version
```

Should show versions like `v18.x.x` and `9.x.x` respectively.

---

## Setup Instructions

### Step 1: Navigate to Backend Folder

```powershell
cd C:\Users\murug\AndroidStudioProjects\BurnOutTracker\cognify-backend
```

### Step 2: Install Dependencies

```powershell
npm install
```

This will install all required packages listed in `package.json`:
- express (web framework)
- sqlite3 (database)
- jsonwebtoken (JWT authentication)
- bcryptjs (password hashing)
- cors (cross-origin requests)
- dotenv (environment variables)
- uuid (unique IDs)
- nodemon (auto-restart on file changes)

### Step 3: Verify Environment Configuration

Check `.env` file:

```env
PORT=5000
JWT_SECRET=cognify_ultra_secure_secret_123!
```

**⚠️ For production, change JWT_SECRET to something secure!**

---

## Starting the Backend

### Option 1: Using Batch File (Windows - Easy) ⭐

**Double-click:** `start-backend.bat`

Or run in PowerShell:
```powershell
.\start-backend.bat
```

### Option 2: Using PowerShell Script (Windows - Advanced)

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\start-backend.ps1
```

### Option 3: Manual Commands (Any Platform)

**Production mode:**
```powershell
npm start
```

**Development mode (auto-restart on file changes):**
```powershell
npm run dev
```

---

## Verification

Once started, you should see:

```
Server is running on port 5000
```

### Test the Backend

**In a new PowerShell window:**

```powershell
Invoke-WebRequest -Uri "http://localhost:5000/"
```

Should return:
```
Cognify Backend API is running...
```

---

## API Endpoints Overview

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/auth/register` | User registration |
| POST | `/api/auth/login` | User login (returns JWT) |
| POST | `/api/burnout/compute` | Calculate burnout risk (requires JWT) |
| POST | `/api/activity/log` | Log physical activity |
| POST | `/api/sleep-mood/log` | Log sleep & mood data |
| GET | `/api/dashboard` | Get dashboard summary |
| POST | `/api/study/log` | Log study session |
| POST | `/api/usage/log` | Log app usage |
| GET | `/api/report/burnout` | Get burnout report |

---

## Database

**Location:** `cognify.db` (SQLite)

**Created automatically on first run with these tables:**
- `users` - User accounts
- `physical_activity` - Steps, calories, activity duration
- `sleep_mood` - Sleep hours and mood scores
- `study_sessions` - Study tracking
- `usage_logs` - App usage analytics
- `productivity` - Focus/escapism metrics

---

## Connection from Android App

### For Android Emulator:
```kotlin
BASE_URL = "http://10.0.2.2:5000/"
```

### For Physical Device (Samsung A35):

1. **Find your computer's IP:**
   ```powershell
   ipconfig
   ```
   Look for IPv4 Address (usually `192.168.x.x`)

2. **Update `RetrofitClient.kt`:**
   ```kotlin
   BASE_URL = "http://192.168.x.x:5000/"  // Replace with your IP
   ```

3. **Ensure phone is on same WiFi network as computer**

---

## Troubleshooting

### ❌ "Node.js not found"
- Install Node.js from https://nodejs.org/
- Restart PowerShell/Command Prompt after installation

### ❌ "Port 5000 already in use"
- Change PORT in `.env`:
  ```env
  PORT=5001
  ```
- Update Android app's `BASE_URL` to match

### ❌ "Database locked"
- Close any other instances of the server
- Delete `cognify.db` to start fresh (⚠️ will lose all data)

### ❌ "npm install fails"
- Clear npm cache:
  ```powershell
  npm cache clean --force
  ```
- Try installing again:
  ```powershell
  npm install
  ```

### ❌ "Cannot GET /api/..."
- Ensure server is running
- Check correct endpoint URL
- Verify JWT token in Authorization header

---

## Stopping the Server

Press **Ctrl+C** in the terminal where the server is running.

---

## Development Tips

**Auto-reload on file changes (dev mode):**
```powershell
npm run dev
```

**View server logs:**
Logs are printed to console in real-time.

**Debug API calls from app:**
- The backend uses HTTP logging interceptor
- Check Android Studio's Logcat for request/response details
- Backend logs all requests to console

---

## Complete Startup Workflow

1. **Open PowerShell as Administrator** (recommended)
2. **Navigate to backend folder:**
   ```powershell
   cd C:\Users\murug\AndroidStudioProjects\BurnOutTracker\cognify-backend
   ```
3. **Start the server:**
   ```powershell
   .\start-backend.bat
   ```
4. **Verify it's running:**
   ```
   Server is running on port 5000
   ```
5. **In Android Studio:**
   - Update `RetrofitClient.kt` with your computer's IP (for physical device)
   - Build and deploy app to Samsung A35
   - Test API calls (login, burnout computation, etc.)

---

## Security Notes

⚠️ **Before Production:**
1. Change `JWT_SECRET` in `.env` to a secure random string
2. Use HTTPS instead of HTTP
3. Add rate limiting to prevent brute force attacks
4. Implement input validation for all API endpoints
5. Use environment-specific configuration files
6. Enable CORS only for trusted domains

---

## Need Help?

- Check server console for error messages
- Verify `.env` configuration
- Ensure Node.js v14+ is installed
- Check that port 5000 is not in use
- Verify Android app's BASE_URL matches server IP:port

---

**Created:** May 9, 2026  
**For:** BurnOut Tracker ML Implementation  
**Version:** 1.0
