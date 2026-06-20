@echo off
REM ============================================================================
REM Cognify Backend Startup Script (Batch)
REM ============================================================================
REM This script starts the Cognify Mental Health Tracker backend server
REM Requirements: Node.js and npm must be installed on your system
REM ============================================================================

cls
echo.
echo =====================================================================
echo   Cognify Backend - Starting Server
echo =====================================================================
echo.

REM Check if Node.js is installed
echo [1/4] Checking Node.js installation...
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js from https://nodejs.org/
    echo.
    pause
    exit /b 1
)
for /f "tokens=*" %%i in ('node --version') do set NODE_VERSION=%%i
echo OK - Node.js found: %NODE_VERSION%
echo.

REM Check if npm is installed
echo [2/4] Checking npm installation...
npm --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: npm is not installed or not in PATH
    echo.
    pause
    exit /b 1
)
for /f "tokens=*" %%i in ('npm --version') do set NPM_VERSION=%%i
echo OK - npm found: %NPM_VERSION%
echo.

REM Install dependencies if node_modules is missing
echo [3/4] Checking/Installing dependencies...
if not exist "node_modules" (
    echo Installing npm packages...
    call npm install
    if errorlevel 1 (
        echo ERROR: Failed to install dependencies
        echo.
        pause
        exit /b 1
    )
    echo OK - Dependencies installed successfully
) else (
    echo OK - Dependencies already installed
)
echo.

REM Start the server
echo [4/4] Starting backend server...
echo.
echo =====================================================================
echo   Server Configuration:
echo   - Port: 5000
echo   - Database: cognify.db (SQLite)
echo   - JWT Secret: Configured from .env
echo =====================================================================
echo.
echo Press Ctrl+C to stop the server
echo.

call npm start

REM Keep window open if there's an error
if errorlevel 1 (
    echo.
    echo ERROR occurred. Press any key to exit...
    pause
)
