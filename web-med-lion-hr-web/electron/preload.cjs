const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("electronAPI", {
  /** Whether running inside Electron (always true when preload runs). */
  isElectron: true,
  /** The OS platform string: 'win32', 'darwin', 'linux', etc. */
  platform: process.platform,
  /** Show a native save dialog and return the chosen path, or null if cancelled. */
  showSaveDialog: (options) => ipcRenderer.invoke("dialog:save", options),
  /** Get the path to the user's data directory (for storing exports). */
  getUserDataPath: () => ipcRenderer.invoke("app:getUserDataPath"),
});
