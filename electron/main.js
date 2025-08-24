import { app, BrowserWindow, Menu } from 'electron';
import path from 'path';
import { spawn } from 'child_process';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const isDev = process.env.NODE_ENV === 'development';

let mainWindow;
let serverProcess;

/**
 * Start the backend server process
 */
function startBackendServer() {
  console.log('🚀 Starting backend server...');
  
  let serverScript;
  let isPackaged = false;
  
  if (isDev) {
    serverScript = path.join(__dirname, '..', 'src', 'server', 'main.ts');
  } else {
    // Check if we're in a packaged app
    isPackaged = __dirname.includes('.app/Contents/Resources');
    if (isPackaged) {
      serverScript = path.join(process.resourcesPath, 'server', 'main.js');
    } else {
      serverScript = path.join(__dirname, '..', 'dist', 'server', 'main.js');
    }
  }

  const command = isDev ? 'tsx' : 'node';
  const args = isDev ? [serverScript] : [serverScript];

  // Set correct working directory for packaged apps
  const cwd = isPackaged 
    ? path.dirname(serverScript)  // Use the server directory as cwd for packaged apps
    : path.join(__dirname, '..');

  serverProcess = spawn(command, args, {
    cwd: cwd,
    stdio: 'inherit',
    env: {
      ...process.env,
      NODE_ENV: isDev ? 'development' : 'production'
    }
  });

  serverProcess.on('error', (err) => {
    console.error('❌ Failed to start server:', err);
  });

  serverProcess.on('exit', (code) => {
    console.log(`🛑 Server process exited with code ${code}`);
  });
}

/**
 * Create the main application window
 */
function createWindow() {
  console.log('🪟 Creating main window...');

  mainWindow = new BrowserWindow({
    width: 480,
    height: 640,
    minWidth: 250,
    minHeight: 200,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      enableRemoteModule: false,
      webSecurity: true
    },
    icon: path.join(__dirname, '..', 'public', 'item_sprites.png'), // Using your existing sprite as icon
    titleBarStyle: 'default',
    resizable: true,
    alwaysOnTop: false, // Set to true if you want it to stay on top while gaming
    show: false // Don't show until ready
  });

  // Load the appropriate URL based on environment
  if (isDev) {
    // Development: load from Vite dev server
    mainWindow.loadURL('http://localhost:3000');
    // Open DevTools only if ELECTRON_DEBUG is set
    if (process.env.ELECTRON_DEBUG) {
      mainWindow.webContents.openDevTools();
    }
  } else {
    // Production: load built React app
    mainWindow.loadFile(path.join(__dirname, '..', 'dist', 'index.html'));
  }

  // Show window when ready to prevent visual flash
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    console.log('✅ Application ready!');
  });

  // Handle window closed
  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  // Handle external links
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    import('electron').then(({ shell }) => shell.openExternal(url));
    return { action: 'deny' };
  });
}

/**
 * Create application menu
 */
function createMenu() {
  const template = [
    {
      label: 'Super Metroid Tracker',
      submenu: [
        {
          label: 'About Super Metroid Tracker',
          role: 'about'
        },
        { type: 'separator' },
        {
          label: 'Quit',
          accelerator: 'CmdOrCtrl+Q',
          click: () => {
            app.quit();
          }
        }
      ]
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { role: 'toggleDevTools' },
        { type: 'separator' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' }
      ]
    },
    {
      label: 'Window',
      submenu: [
        { role: 'minimize' },
        { role: 'close' },
        { type: 'separator' },
        {
          label: 'Always on Top',
          type: 'checkbox',
          checked: false,
          accelerator: 'CmdOrCtrl+T',
          click: (menuItem) => {
            if (mainWindow) {
              mainWindow.setAlwaysOnTop(menuItem.checked);
            }
          }
        }
      ]
    }
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
}

/**
 * App event handlers
 */
app.whenReady().then(async () => {
  console.log('🎮 Super Metroid Tracker starting...');
  
  // Create menu
  createMenu();
  
  // Start backend server
  startBackendServer();
  
  // Wait a moment for server to start, then create window
  setTimeout(() => {
    createWindow();
  }, isDev ? 3000 : 2000); // Longer wait in dev for Vite to start

  // macOS: Re-create window when dock icon clicked
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

// Quit when all windows are closed (except on macOS)
app.on('window-all-closed', () => {
  console.log('🛑 All windows closed, cleaning up...');
  
  // Kill server process
  if (serverProcess) {
    console.log('🔥 Terminating server process...');
    serverProcess.kill('SIGTERM');
    
    // Force kill after timeout
    setTimeout(() => {
      if (serverProcess && !serverProcess.killed) {
        console.log('⚡ Force killing server process...');
        serverProcess.kill('SIGKILL');
      }
    }, 5000);
  }

  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// Security: Prevent new window creation
app.on('web-contents-created', (event, contents) => {
  contents.on('new-window', (event, navigationUrl) => {
    event.preventDefault();
    import('electron').then(({ shell }) => shell.openExternal(navigationUrl));
  });
});

// Handle app termination
app.on('before-quit', () => {
  console.log('🛑 App is quitting...');
  
  if (serverProcess) {
    serverProcess.kill('SIGTERM');
  }
});

// Prevent navigation to external URLs
app.on('web-contents-created', (event, contents) => {
  contents.on('will-navigate', (event, navigationUrl) => {
    const parsedUrl = new URL(navigationUrl);
    
    // Allow localhost navigation in development
    if (isDev && parsedUrl.origin === 'http://localhost:3000') {
      return;
    }
    
    // Allow file protocol for production
    if (parsedUrl.protocol === 'file:') {
      return;
    }
    
    // Block all other navigation
    event.preventDefault();
  });
});