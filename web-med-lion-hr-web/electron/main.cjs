const { app, BrowserWindow, Menu, nativeImage, Tray, dialog, ipcMain } = require("electron");
const path = require("path");
const fs = require("fs");

/** @type {BrowserWindow | null} */
let mainWindow = null;
/** @type {Tray | null} */
let tray = null;

const isDev = !app.isPackaged;

const WINDOW_STATE_PATH = path.join(app.getPath("userData"), "window-state.json");

function loadWindowState() {
  try {
    if (fs.existsSync(WINDOW_STATE_PATH)) {
      const raw = fs.readFileSync(WINDOW_STATE_PATH, "utf-8");
      const state = JSON.parse(raw);
      if (state && typeof state.width === "number" && typeof state.height === "number") {
        return {
          width: Math.max(state.width, 900),
          height: Math.max(state.height, 600),
          x: typeof state.x === "number" ? state.x : undefined,
          y: typeof state.y === "number" ? state.y : undefined,
        };
      }
    }
  } catch (_) { /* ignore corrupt state */ }
  return { width: 1280, height: 800 };
}

function saveWindowState() {
  if (!mainWindow) return;
  try {
    const bounds = mainWindow.getBounds();
    fs.writeFileSync(WINDOW_STATE_PATH, JSON.stringify(bounds));
  } catch (_) { /* ignore */ }
}

function getAppIcon() {
  const iconPath = path.join(__dirname, "..", "public", "icon.png");
  try {
    if (fs.existsSync(iconPath)) return nativeImage.createFromPath(iconPath);
  } catch (_) { /* ignore */ }
  return undefined;
}

function createTray() {
  const icon = getAppIcon();
  if (!icon) return;
  const trayIcon = icon.resize({ width: 16, height: 16 });
  tray = new Tray(trayIcon);
  tray.setToolTip("Med Lion HR");
  tray.setContextMenu(
    Menu.buildFromTemplate([
      {
        label: "Show",
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            mainWindow.focus();
          }
        },
      },
      { type: "separator" },
      {
        label: "Quit",
        click: () => {
          app.quit();
        },
      },
    ])
  );
  tray.on("double-click", () => {
    if (mainWindow) {
      mainWindow.show();
      mainWindow.focus();
    }
  });
}

function createWindow() {
  const windowState = loadWindowState();

  mainWindow = new BrowserWindow({
    width: windowState.width,
    height: windowState.height,
    x: windowState.x,
    y: windowState.y,
    minWidth: 900,
    minHeight: 600,
    title: "Med Lion HR",
    icon: getAppIcon(),
    backgroundColor: "#0a0a0f",
    show: false,
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  });

  // Save window state on move/resize
  mainWindow.on("resize", saveWindowState);
  mainWindow.on("move", saveWindowState);
  mainWindow.on("close", saveWindowState);

  mainWindow.once("ready-to-show", () => {
    mainWindow?.show();
  });

  // Minimize to tray instead of closing
  mainWindow.on("close", (event) => {
    if (tray && !app.isQuitting) {
      event.preventDefault();
      mainWindow?.hide();
    }
  });

  mainWindow.on("closed", () => {
    mainWindow = null;
  });

  // Remove default menu bar for cleaner look
  Menu.setApplicationMenu(null);

  if (isDev) {
    mainWindow.loadURL("http://localhost:5173");
  } else {
    mainWindow.loadFile(path.join(__dirname, "..", "dist", "index.html"));
  }
}

// Prevent multiple instances
const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
} else {
  app.on("second-instance", () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
    }
  });
}

app.whenReady().then(() => {
  createWindow();
  createTray();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("before-quit", () => {
  app.isQuitting = true;
});

// IPC handlers
ipcMain.handle("dialog:save", async (_event, options) => {
  const { filePath } = await dialog.showSaveDialog(mainWindow, {
    defaultPath: options?.defaultPath || "export.csv",
    filters: options?.filters || [{ name: "All Files", extensions: ["*"] }],
  });
  return filePath || null;
});

ipcMain.handle("app:getUserDataPath", () => {
  return app.getPath("userData");
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
