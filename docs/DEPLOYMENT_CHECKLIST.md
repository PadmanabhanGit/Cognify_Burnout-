# ✅ BurnOut Tracker - Deployment Checklist

## Phase 1: Backend (✅ COMPLETE)

- [x] Node.js installed and working
- [x] npm dependencies installed
- [x] Database initialized (cognify.db)
- [x] Server running on port 5000
- [x] All API routes configured
- [x] JWT authentication working
- [x] ML algorithm verified (fixed bug on line 116)
- [x] Startup scripts created

---

## Phase 2: Android App Configuration ( IN PROGRESS)

### Step 2.1: Get Your Computer's IP Address

- [ ] Open PowerShell
- [ ] Run: `ipconfig`
- [ ] Find: IPv4 Address (write it down)
- [ ] Example: `192.168.1.100`

**Your IP:** ________________

---

### Step 2.2: Update RetrofitClient.kt

- [ ] Open Android Studio
- [ ] Navigate to: `app/src/main/java/com/simats/burnouttracker/data/api/RetrofitClient.kt`
- [ ] Find line 20
- [ ] Change: `private const val BASE_URL = "http://10.0.2.2:5000/"`
- [ ] To: `private const val BASE_URL = "http://YOUR_IP:5000/"`
- [ ] Replace `YOUR_IP` with your actual IP (e.g., `192.168.1.100`)
- [ ] Save file (Ctrl+S)
- [ ] Verify no errors appear

---

### Step 2.3: Build the App

- [ ] In Android Studio, go to: `Build` → `Clean Project`
- [ ] Wait for clean to complete
- [ ] Go to: `Build` → `Build Bundle(s)/APK(s)` → `Build APK(s)`
- [ ] Wait for build to complete
- [ ] Verify: **BUILD SUCCESSFUL** message appears ✓

---

## Phase 3: Deploy to Samsung A35 (⏭️ NEXT)

### Step 3.1: Prepare Phone

- [ ] Connect Samsung A35 to computer via USB cable
- [ ] On phone, open: `Settings` → `About Phone`
- [ ] Tap "Build Number" 7 times to enable Developer Options
- [ ] Open: `Settings` → `Developer Options`
- [ ] Enable: `USB Debugging`
- [ ] Accept RSA security prompts on phone

### Step 3.2: Deploy App

**Option A: Via Android Studio (Recommended)**
- [ ] Click green **Run** button (play icon) in Android Studio
- [ ] Select "Samsung A35" from device list
- [ ] Click **OK**
- [ ] Wait for app to install and launch on phone

**Option B: Manual APK Installation**
- [ ] Find APK: `app/build/outputs/apk/debug/app-debug.apk`
- [ ] Copy APK to phone (USB transfer or email yourself)
- [ ] On phone, open File Manager
- [ ] Navigate to APK location
- [ ] Tap APK and select "Install"
- [ ] Wait for installation to complete

---

## Phase 4: Testing (✅ TO VERIFY)

### Step 4.1: App Launch

- [ ] Open the app on Samsung A35
- [ ] Verify app launches without crashing
- [ ] Login screen should appear
- [ ] Check Android Studio Logcat for errors (none expected)

### Step 4.2: User Registration

- [ ] Tap "Sign Up" (or register button)
- [ ] Enter email: `test@example.com`
- [ ] Enter password: `password123`
- [ ] Enter name: `Test User`
- [ ] Tap "Create Account"
- [ ] Verify success message appears

### Step 4.3: User Login

- [ ] Tap "Sign In"
- [ ] Enter email: `test@example.com`
- [ ] Enter password: `password123`
- [ ] Tap "Sign In"
- [ ] Verify logged in and JWT token saved

### Step 4.4: Burnout Risk Screen

- [ ] Navigate to "Burnout Risk" screen
- [ ] Verify screen loads without error
- [ ] Should display burnout risk score (0-100)
- [ ] Should show risk level (Low/Medium/High/Critical)
- [ ] Check for correct weighted factors:
  - [ ] Sleep quality contribution
  - [ ] Physical activity contribution
  - [ ] Focus/escapism balance contribution
  - [ ] Mood score contribution

### Step 4.5: Backend Verification

- [ ] Check backend console for API requests
- [ ] Should see POST requests to:
  - [ ] `/api/auth/register`
  - [ ] `/api/auth/login`
  - [ ] `/api/burnout/compute`
- [ ] All requests should return status 200 (success)
- [ ] No 401 (unauthorized) or 500 (server error) messages

---

## Phase 5: Troubleshooting (❌ IF ISSUES)

### Issue: App won't install to phone

**Checklist:**
- [ ] Phone has at least 100MB free space
- [ ] USB Debugging is enabled on phone
- [ ] USB cable is working (try different USB port)
- [ ] Accept any security prompts on phone
- [ ] Try: `adb kill-server` then `adb start-server`

**If still failing:**
```powershell
cd "C:\Users\murug\AndroidStudioProjects\BurnOutTracker"
adb devices  # Should show your device
adb install "app\build\outputs\apk\debug\app-debug.apk"
```

---

### Issue: App launches but crashes immediately

**Checklist:**
- [ ] Check Android Logcat for error message
- [ ] Verify backend is running: `netstat -ano | findstr ":5000"`
- [ ] TensorFlow Lite version is 2.14.0 (not LiteRT)
- [ ] useLegacyPackaging is set to true

**If still crashing:**
```powershell
adb logcat | findstr "E/" # Show only errors
# Restart app and watch for error messages
```

---

### Issue: "Failed to connect to backend"

**Checklist:**
- [ ] Backend server is running
- [ ] RetrofitClient.kt has correct IP address
- [ ] Phone is on same WiFi network as computer
- [ ] Windows Firewall allows port 5000

**Quick test:**
```powershell
# On phone, open Chrome and visit:
http://192.168.1.100:5000/
# Should show: "Cognify Backend API is running..."
```

---

### Issue: Login fails with "401 Unauthorized"

**Checklist:**
- [ ] Backend auth middleware is working
- [ ] User was successfully registered
- [ ] Correct email and password are used
- [ ] AuthInterceptor is attached to requests

**Test:**
```powershell
# Test login manually:
$body = '{"email":"test@example.com","password":"password123"}' | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:5000/api/auth/login" `
  -Method POST -Headers @{"Content-Type"="application/json"} -Body $body
```

---

### Issue: Burnout score is 0 or missing data

**Checklist:**
- [ ] User has been logged in for at least one day
- [ ] Physical activity data exists (check database)
- [ ] Sleep and mood data logged
- [ ] No errors in backend console

**Reset and test:**
```powershell
# Delete database and start fresh:
cd cognify-backend
del cognify.db  # Deletes all data
npm start       # Recreates database
```

---

## Phase 6: Success Criteria ✨

The deployment is **SUCCESSFUL** when:

- ✅ App launches on Samsung A35 without crashing
- ✅ User can register with email/password
- ✅ User can login and receives JWT token
- ✅ Burnout Risk screen loads and displays score
- ✅ Score is calculated from all 4 weighted factors
- ✅ Backend console shows all API requests with 200 status
- ✅ No errors in Android Logcat
- ✅ No errors in backend console
- ✅ Phone connects via WiFi to backend server
- ✅ Data persists in SQLite database

---

##  Quick Reference Commands

### Start Backend
```powershell
cd cognify-backend
start-backend.bat
```

### Check Backend Status
```powershell
Invoke-WebRequest -Uri "http://localhost:5000/"
```

### Stop Backend
```powershell
cognify-backend\stop-backend.bat
```

### View Android Logs
```powershell
adb logcat | findstr "burnouttracker"
```

### Clear App Data
```powershell
adb shell pm clear com.simats.burnouttracker
```

### Check Port Usage
```powershell
netstat -ano | findstr ":5000"
```

### Get Your IP
```powershell
ipconfig | findstr "IPv4"
```

---

##  Go / No-Go Decision Points

### Before deploying app:
- [ ] Backend is running and responding (test with Invoke-WebRequest)
- [ ] RetrofitClient.kt has been updated with your IP
- [ ] App builds successfully with BUILD SUCCESSFUL message
- [ ] No compile errors in Android Studio

### Before testing on phone:
- [ ] Phone has USB Debugging enabled
- [ ] Phone is on same WiFi as computer
- [ ] Backend is still running on port 5000
- [ ] No other apps are using port 5000

### After app install:
- [ ] App opens without immediate crash
- [ ] Login screen is visible
- [ ] No permission errors are shown
- [ ] Logcat shows connection attempts to backend

---

##  Need Help?

1. **Check the detailed guides:**
   - `COMPLETE_APP_STARTUP_GUIDE.md` - Full instructions
   - `BACKEND_STARTUP_GUIDE.md` - Backend details
   - `INITIALIZATION_SUMMARY.md` - Overall status

2. **Run diagnostic script:**
   ```powershell
   cognify-backend\QUICK_SETUP.bat
   ```

3. **Check logs:**
   - Android Studio → Logcat → Filter for errors
   - Backend console → Look for API request logs
   - PowerShell → `netstat -ano | findstr ":5000"`

4. **Test endpoints manually:**
   - See `COMPLETE_APP_STARTUP_GUIDE.md` for PowerShell test commands

---

##  Current Status Dashboard

```
Backend:          RUNNING
Database:         INITIALIZED  
API Routes:       CONFIGURED
ML Algorithm:     FIXED & READY
Android Build:    READY TO DEPLOY
Phone Setup:     ⏳ PENDING (next step)
IP Configuration:⏳ PENDING (next step)
Final Testing:   ⏳ PENDING (final step)

Overall: 70% Complete - Ready for Android Testing Phase
```

---

##  Notes

- **Keep this checklist nearby** while deploying
- **Check off items** as you complete them
- **Don't skip steps** - each builds on the previous
- **If stuck**, see troubleshooting section
- **Save your computer's IP** in the space above

---

**Start Date:** May 9, 2026  
**Target Completion:** May 9, 2026 (same day testing)  
**Status:** Ready for Phase 2 (Android Configuration)
