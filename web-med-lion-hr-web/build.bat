@echo off
setlocal enabledelayedexpansion
title Med Lion HR - Windows Installer Builder

echo ============================================
echo   Med Lion HR - Windows Desktop App Builder
echo ============================================
echo.

REM ── Check if Bun is installed ──
where bun >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Bun is not installed.
    echo.
    echo Install Bun first:
    echo   powershell -c "irm bun.sh/install.ps1 ^| iex"
    echo.
    echo Then re-open your terminal and run this script again.
    pause
    exit /b 1
)

echo Bun found: 
bun --version
echo.

REM ── Step 1: Install dependencies ──
echo [1/4] Installing dependencies...
call bun install --frozen-lockfile 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Lockfile missing or outdated. Running bun install...
    call bun install
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to install dependencies.
        pause
        exit /b 1
    )
)
echo        Done.
echo.

REM ── Step 2: Download Electron binary ──
echo [2/4] Checking Electron binary...
if not exist "node_modules\electron\dist\electron.exe" (
    echo        Downloading Electron runtime (~100MB)...
    node node_modules\electron\install.js
    if %ERRORLEVEL% NEQ 0 (
        echo [WARNING] Electron binary download failed.
        echo        The build may still work if electron-builder downloads it.
    ) else (
        echo        Done.
    )
) else (
    echo        Already installed.
)
echo.

REM ── Step 3: Build the web app ──
echo [3/4] Building web app...
call bun run build
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Vite build failed. Check errors above.
    pause
    exit /b 1
)
echo        Done.
echo.

REM ── Step 4: Package as Windows installer ──
echo [4/4] Packaging Windows installer...
echo        This will produce:
echo          - NSIS Setup installer (Med Lion HR Setup x.x.x.exe^)
echo          - Portable standalone (Med Lion HR x.x.x.exe^)
echo.
call bunx electron-builder --win
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] electron-builder failed.
    echo.
    echo Common fixes:
    echo   1. Make sure you have enough disk space
    echo   2. Try running: node node_modules\electron\install.js
    echo   3. Delete node_modules and run build.bat again
    pause
    exit /b 1
)

echo.
echo ============================================
echo   BUILD COMPLETE!
echo.
echo   Your EXE files are in the "release" folder:
echo     %CD%\release\
echo.
echo   ^> Setup installer:  "Med Lion HR Setup x.x.x.exe"
echo      (Share this with others - they can install it)
echo.
echo   ^> Portable:         "Med Lion HR x.x.x.exe"
echo      (Runs without installation - keep on USB)
echo ============================================
echo.
echo   LAUNCH INSTRUCTIONS
echo   -------------------
echo   1. Start the server (in a separate terminal):
echo      cd server
echo      bun run src/index.ts
echo.
echo   2. Install or run the portable EXE
echo   3. The app will connect to http://localhost:8080
echo ============================================
echo.
pause
