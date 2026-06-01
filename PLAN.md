# Convert Web Dashboard to Windows Electron App

## What Will Change

The current web dashboard (which runs in a browser) will be converted into a standalone Windows desktop application.

## Features
- [x] Same admin dashboard UI — login, live workforce view, leave approvals, payroll, employee management
- [x] Runs as a native Windows window with its own icon, title bar, and taskbar entry
- [x] Connects to the same local server as the Android app
- [x] System tray support for quick access
- [x] Window remembers its size and position between launches
- [x] Production EXE installer (NSIS) + portable EXE via electron-builder
- [x] API client auto-detects dev vs production and uses correct server URL

## Design
- [x] All existing dark glassmorphic styling stays the same
- [x] Proper Windows window chrome replacing the browser tab
- [x] App icon in the taskbar and start menu

## Implementation Details
- [x] Added Electron + electron-builder dependencies
- [x] Created `electron/main.cjs` — main process with window management, tray, IPC
- [x] Created `electron/preload.cjs` — secure context bridge
- [x] Updated `vite.config.ts` — base path + port fix
- [x] Updated `package.json` — main entry, scripts, electron-builder config
- [x] Fixed `api.ts` — production Electron uses `http://localhost:8080` as API base
- [x] Created `build.bat` — one-click Windows build script
- [x] Build validated

## How to Build the Windows EXE
1. Open a terminal in `web-med-lion-hr-web`
2. Run: `build.bat`
3. Find the installer in: `release\Med Lion HR Setup x.x.x.exe`

## How to Run
1. Start the server: `cd server && bun run src/index.ts`
2. Start the Electron dev app: `cd web-med-lion-hr-web && bun run electron:dev`
3. Or build for production: `bun run electron:build`

## What Happens to the Web App
The existing web dashboard has been converted to an Electron desktop app. The server remains unchanged — both the Android app and the Windows app connect to it the same way.
