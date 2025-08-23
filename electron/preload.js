// Preload script for enhanced security
// This script runs in a secure context before the web content loads

const { contextBridge, ipcRenderer } = require('electron');

// Expose safe APIs to the renderer process if needed
// For now, we don't need any special APIs since the app communicates via HTTP
contextBridge.exposeInMainWorld('electronAPI', {
  // Example: Get app version
  getVersion: () => ipcRenderer.invoke('get-version'),
  
  // Example: Platform info
  getPlatform: () => process.platform,
  
  // Example: Check if running in Electron
  isElectron: () => true
});

console.log('🔒 Preload script loaded');