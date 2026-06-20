# ============================================================================
# Cognify Backend Startup Script (PowerShell)
# ============================================================================
# This script starts the Cognify Mental Health Tracker backend server
# Requirements: Node.js and npm must be installed on your system
# ============================================================================

Write-Host "═════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Cognify Backend - Starting Server" -ForegroundColor Cyan
Write-Host "═════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Check if Node.js is installed
Write-Host "[1/4] Checking Node.js installation..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version
    Write-Host "✓ Node.js found: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Node.js is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Node.js from https://nodejs.org/" -ForegroundColor Red
    exit 1
}

# Check if npm is installed
Write-Host "[2/4] Checking npm installation..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version
    Write-Host "✓ npm found: $npmVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ npm is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Install dependencies if node_modules is missing
Write-Host "[3/4] Checking/Installing dependencies..." -ForegroundColor Yellow
if (!(Test-Path "node_modules")) {
    Write-Host "  Installing npm packages..." -ForegroundColor Cyan
    npm install
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Dependencies installed successfully" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to install dependencies" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✓ Dependencies already installed" -ForegroundColor Green
}

# Start the server
Write-Host "[4/4] Starting backend server..." -ForegroundColor Yellow
Write-Host ""
Write-Host "═════════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "  Server Configuration:" -ForegroundColor Green
Write-Host "  • Port: 5000" -ForegroundColor Green
Write-Host "  • Database: cognify.db (SQLite)" -ForegroundColor Green
Write-Host "  • JWT Secret: Configured from .env" -ForegroundColor Green
Write-Host "═════════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""

npm start

# Keep window open if there's an error
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "An error occurred. Press any key to exit..." -ForegroundColor Red
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
