@echo off
echo ============================================
echo   Med Lion HR - Windows Installer Builder
echo ============================================
echo.

REM Check if Bun is installed
where bun >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Bun is not installed.
    echo Install it from https://bun.sh
    echo Or run: powershell -c "irm bun.sh/install.ps1 | iex"
    pause
    exit /b 1
)

echo [1/3] Installing dependencies...
call bun install
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to install dependencies.
    pause
    exit /b 1
)

echo.
echo [2/3] Downloading Electron binary...
call node node_modules/electron/install.js
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] Electron binary download had issues.
    echo Trying to continue anyway...
)

echo.
echo [3/3] Building Windows installer...
call npx electron-builder --win
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed. Check the logs above.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   BUILD COMPLETE!
echo.
echo   Installer: release\Med Lion HR Setup *.exe
echo   Portable:  release\Med Lion HR *.exe
echo ============================================
echo.
echo Share the Setup exe with others.
echo The portable exe runs without installation.
pause
