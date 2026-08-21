@echo off
REM ============================================================================
REM Cognify Backend Server - Stop/Kill Process
REM ============================================================================
REM This script stops the currently running backend server on port 5000
REM ============================================================================

cls
echo.
echo =====================================================================
echo   Cognify Backend - Stop Server
echo =====================================================================
echo.

REM Check if anything is running on port 5000
netstat -ano | findstr ":5000" >nul
if errorlevel 1 (
    echo INFO: No process found running on port 5000
    echo.
    pause
    exit /b 0
)

echo Processes on port 5000:
netstat -ano | findstr ":5000"
echo.

REM Get process ID from netstat output
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5000"') do (
    set PID=%%a
)

if defined PID (
    echo Attempting to stop process with PID: %PID%
    taskkill /PID %PID% /F
    if errorlevel 1 (
        echo ERROR: Failed to kill process
        pause
        exit /b 1
    ) else (
        echo SUCCESS: Process terminated
        echo.
        pause
        exit /b 0
    )
) else (
    echo ERROR: Could not determine process ID
    pause
    exit /b 1
)
