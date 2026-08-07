@echo off
REM ============================================================================
REM Cognify - Quick Setup Assistant
REM ============================================================================
REM This script helps configure the Android app to connect to the backend
REM ============================================================================

cls
setlocal enabledelayedexpansion

echo.
echo =====================================================================
echo   Cognify App - Quick Setup Assistant
echo =====================================================================
echo.

REM Check if backend is running
echo [Step 1] Checking if backend server is running...
netstat -ano | findstr ":5000" >nul
if errorlevel 1 (
    echo WARNING: No process found on port 5000
    echo Please start the backend first:
    echo   1. Open Command Prompt in cognify-backend folder
    echo   2. Run: npm start  OR  start-backend.bat
    echo.
    pause
    exit /b 1
) else (
    echo OK - Backend is running on port 5000
)
echo.

REM Get local IP address
echo [Step 2] Finding your computer's local IP address...
echo.
echo Your network adapters:
echo =====================
ipconfig | findstr /R "IPv4 Address.*[0-9]"
echo.
echo =====================
echo.
echo Copy the IPv4 Address from above (usually 192.168.x.x or 10.x.x.x)
echo.
set /p IP_ADDRESS="Enter your computer's IPv4 address: "

if "!IP_ADDRESS!"=="" (
    echo ERROR: No IP address provided
    pause
    exit /b 1
)

echo.
echo You entered: !IP_ADDRESS!
echo.

REM Display next steps
cls
echo.
echo =====================================================================
echo   NEXT STEPS - Update Your Android App
echo =====================================================================
echo.
echo 1. Open Android Studio
echo    File ^> Open ^> BurnOutTracker project
echo.
echo 2. Navigate to file:
echo    app/src/main/java/com/simats/burnouttracker/data/api/RetrofitClient.kt
echo.
echo 3. Find line 20:
echo    private const val BASE_URL = "http://10.0.2.2:5000/"
echo.
echo 4. Replace with:
echo    private const val BASE_URL = "http://!IP_ADDRESS!:5000/"
echo.
echo 5. Save the file (Ctrl+S)
echo.
echo 6. Build and deploy to Samsung A35:
echo    - Run ^> Run 'app'
echo    - Select your device from the list
echo.
echo =====================================================================
echo   IMPORTANT NOTES
echo =====================================================================
echo.
echo ✓ Your phone must be on the SAME WiFi network
echo ✓ Backend must be running before starting the app
echo ✓ If you get connection errors:
echo   - Check WiFi connection
echo   - Check Windows Firewall settings
echo   - Run: netsh advfirewall firewall add rule name="Node.js Port 5000" 
echo       dir=in action=allow protocol=tcp localport=5000
echo.
echo ✓ To verify backend is accessible:
echo   - On phone, open web browser
echo   - Go to: http://!IP_ADDRESS!:5000/
echo   - Should see: "Cognify Backend API is running..."
echo.
echo ✓ Backend IP: http://!IP_ADDRESS!:5000/
echo.
echo =====================================================================
echo.
pause
