#  Cognify - Complete App Startup Guide

## Current Status ✅

```
✓ Backend Server: RUNNING on http://localhost:5000/
✓ Database: cognify.db (SQLite) - Initialized
✓ Dependencies: All installed
✓ Node.js: v24.14.1 installed
✓ Port 5000: Available
```

---

##  Quick Start Workflow

### Phase 1: Backend Initialization (DONE ✅)

The backend is already running. You can verify anytime:

```powershell
Invoke-WebRequest -Uri "http://localhost:5000/" | Select-Object -ExpandProperty Content
```

Should return: `Cognify Backend API is running...`

---

### Phase 2: Android App Setup & Deployment

#### Step 1: Update RetrofitClient with Your Computer's IP

Since you're using a physical device (Samsung A35), you need to update the backend URL.

**Find your computer's IP:**
```powershell
ipconfig
```

Look for: **IPv4 Address** (usually `192.168.x.x` or `10.x.x.x`)

**Update the file:**
File: `C:\Users\murug\AndroidStudioProjects\BurnOutTracker\app\src\main\java\com\simats\burnouttracker\data\api\RetrofitClient.kt`

Change line 20:
```kotlin
// BEFORE (for emulator):
private const val BASE_URL = "http://10.0.2.2:5000/"

// AFTER (for physical device):
private const val BASE_URL = "http://192.168.x.x:5000/"  // ← Use YOUR IP
```

**⚠️ Important:**
- Your phone and computer must be on the **same WiFi network**
- If using 4G/mobile data, port forwarding may be needed
- For testing, use the same network

---

#### Step 2: Build the Android App

In Android Studio:

1. **Menu:** `Build` → `Clean Project`
2. **Menu:** `Build` → `Build Bundle(s)/APK(s)` → `Build APK(s)`
3. Wait for build to complete

**Expected:** Green checkmark "BUILD SUCCESSFUL"

---

#### Step 3: Deploy to Samsung A35

**Option A: Via Android Studio (Easiest)**

1. Connect phone to computer via USB cable
2. Enable **USB Debugging** on phone:
   - Settings → Developer Options → USB Debugging → ON
3. In Android Studio, click **Run** (green play button)
4. Select your device from the list

**Option B: Manual APK Installation**

1. Build APK (see Step 2)
2. Locate APK: `app/build/outputs/apk/debug/app-debug.apk`
3. Transfer to phone (USB or any method)
4. Open file manager on phone
5. Tap `app-debug.apk` and install

---

#### Step 4: Test the Complete Flow

On your Samsung A35:

1. **Open the app** (should not crash)
2. **Register/Login:**
   - Email: `test@example.com`
   - Password: `password123`
   - Click "Sign In"

3. **Navigate to Burnout Screen**
   - The app should fetch burnout risk data from backend
   - Should show risk score (0-100)

4. **Check Android Studio Logcat** for any errors

---

##  Testing Different Scenarios

### Scenario 1: Test Backend API Directly

```powershell
# Test root endpoint
Invoke-WebRequest -Uri "http://localhost:5000/"

# Register user
$body = @{
    email = "test@example.com"
    password = "password123"
    name = "Test User"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:5000/api/auth/register" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

$response.Content | ConvertFrom-Json
```

### Scenario 2: Test Login & Get JWT Token

```powershell
$loginBody = @{
    email = "test@example.com"
    password = "password123"
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest -Uri "http://localhost:5000/api/auth/login" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $loginBody

$token = ($loginResponse.Content | ConvertFrom-Json).token
Write-Host "JWT Token: $token"
```

### Scenario 3: Test Burnout Computation (Requires JWT)

```powershell
# First get token (see Scenario 2), then:

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

$burnoutResponse = Invoke-WebRequest -Uri "http://localhost:5000/api/burnout/compute" `
    -Method POST `
    -Headers $headers

$burnoutResponse.Content | ConvertFrom-Json | Format-List
```

---

##  Backend Control Scripts

Located in: `cognify-backend/`

### Start Backend
```powershell
.\start-backend.bat
```
OR
```powershell
npm start
```

### Stop Backend
```powershell
.\stop-backend.bat
```

### Development Mode (Auto-reload)
```powershell
npm run dev
```

---

## ️ Project Structure

```
BurnOutTracker/
├── cognify-backend/                    ← Backend Server
│   ├── server.js                       ← Express app entry point
│   ├── database.js                     ← SQLite initialization
│   ├── package.json                    ← Dependencies
│   ├── .env                            ← Configuration
│   ├── middleware/
│   │   └── auth.js                     ← JWT validation
│   ├── routes/                         ← API endpoints
│   │   ├── auth.js                     ← Login/Register
│   │   ├── burnout.js                  ← ML algorithm
│   │   ├── physicalActivity.js
│   │   ├── sleepMood.js
│   │   ├── study.js
│   │   ├── dashboard.js
│   │   └── ...
│   └── start-backend.bat               ← Startup script
│
└── app/                                ← Android Kotlin App
    ├── src/main/java/...
    │   ├── LoginScreen.kt
    │   ├── BurnoutRiskScreen.kt
    │   └── data/api/
    │       ├── RetrofitClient.kt       ← Base URL here!
    │       ├── AuthInterceptor.kt
    │       └── ApiService.kt
    ├── src/main/assets/
    │   ├── app_classifier.tflite       ← ML model
    │   └── vocab.txt                   ← ML vocab
    └── build.gradle.kts                ← Build config
```

---

## ⚠️ Troubleshooting

### Problem: "Failed to connect to localhost:5000"

**Solution:**
- Backend not running?
  ```powershell
  .\start-backend.bat
  ```
- Port is blocked?
  ```powershell
  netstat -ano | findstr ":5000"
  .\stop-backend.bat
  .\start-backend.bat
  ```

### Problem: Physical device can't reach backend

**Solution:**
- Update RetrofitClient.kt with your computer's IP
- Ensure both are on same WiFi network
- Check Windows Firewall allows port 5000:
  ```powershell
  netsh advfirewall firewall add rule name="Node.js Port 5000" `
    dir=in action=allow protocol=tcp localport=5000
  ```

### Problem: "Port 5000 already in use"

**Solution:**
```powershell
.\stop-backend.bat
.\start-backend.bat
```

Or change port in `.env`:
```env
PORT=5001
```
Then update Android app's RetrofitClient to use `5001`

### Problem: Authentication fails (401 error)

**Solution:**
- Clear app data on Samsung A35
- Logout and login again
- Check that JWT token is being saved to SharedPreferences
- Verify backend auth middleware is working

### Problem: App crashes on launch

**Solution (already applied):**
- ✓ TensorFlow Lite downgraded to 2.14.0
- ✓ useLegacyPackaging = true
- ✓ 16KB alignment issue fixed
- If still crashing:
  1. Check Logcat for exact error
  2. Try clearing app cache: `adb shell pm clear com.simats.burnouttracker`
  3. Rebuild and redeploy

---

##  Pre-Launch Checklist

- [ ] Backend is running (`npm start` or `start-backend.bat`)
- [ ] Backend responds to: `http://localhost:5000/`
- [ ] RetrofitClient.kt has correct IP address for physical device
- [ ] Samsung A35 is on same WiFi as computer
- [ ] USB Debugging enabled on Samsung A35
- [ ] APK built successfully in Android Studio
- [ ] App deploys to Samsung A35 without crashing
- [ ] User can register new account
- [ ] User can login
- [ ] Burnout risk screen loads and displays data
- [ ] No JWT authentication errors in console

---

##  Expected Results

### After Starting Backend:
```
Server is running on port 5000
✓ All API routes initialized
✓ SQLite database ready
✓ JWT middleware active
```

### After Deploying App:
```
✓ App launches without crash on Samsung A35
✓ Login screen appears
✓ Can register new user
✓ Can login with credentials
✓ Burnout risk screen shows computed score
✓ All data flows from app → backend → database → response
```

---

##  Security Notes

⚠️ **Current Setup (Development):**
- JWT Secret: `cognify_ultra_secure_secret_123!` (from .env)
- HTTP only (not HTTPS)
- No rate limiting
- No input validation on some endpoints

⚠️ **Before Production:**
1. Change JWT_SECRET to strong random value
2. Enable HTTPS/SSL certification
3. Add rate limiting for login attempts
4. Validate all inputs
5. Use environment-specific configs
6. Enable logging/monitoring
7. Add CORS whitelist with specific domains

---

##  Support

If you encounter issues:

1. **Check Logcat in Android Studio** for error messages
2. **Check backend console** for API errors
3. **Verify WiFi connection** between phone and computer
4. **Check firewall settings** for port 5000
5. **Review backend logs:**
   ```powershell
   cd cognify-backend
   npm run dev  # Shows detailed logs
   ```

---

##  Next Steps

1. ✅ **Backend Ready** - Already running
2. **Update RetrofitClient.kt** - Change IP address
3. **Build & Deploy** - Get APK on Samsung A35
4. **Test API Flow** - Register, login, compute burnout
5. **Verify ML Integration** - Check all 4 weighted factors
6. **Implement Auto-Sync** - Samsung Health/Google Fit integration
7. **Schedule Background Tasks** - Daily burnout computation
8. **Deploy to Production** - Move to HTTPS and secure configuration

---

**Last Updated:** May 9, 2026  
**Status:** ✅ Backend Running, Ready for Android Testing  
**ML Implementation:** ✅ Complete  
**Device Compatibility:** ✅ Samsung A35 supported (16KB alignment fixed)
