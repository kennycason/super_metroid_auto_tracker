# Super Metroid Live Tracker - Unified Web App

A real-time Super Metroid item and boss tracker that reads game memory from RetroArch via UDP. **Now unified as a single web application** with TypeScript backend and React frontend!

## 🚀 What's New - Unified Architecture

This project has been **migrated from separate backend + frontend** to a **unified web application**:

- ✅ **TypeScript Backend** - Migrated from Kotlin/Python to TypeScript (Node.js)
- ✅ **React Frontend** - Modern React 19 with TypeScript
- ✅ **Single Application** - One-stop shop to run everything
- ✅ **Perfect Migration** - All functionality preserved from original backends
- ✅ **Comprehensive Tests** - Full test coverage with Vitest

## Features

- 🎮 **Live Game Tracking** - Real-time item and boss status
- 🔧 **RetroArch Integration** - Connects via UDP to read game memory  
- 🌐 **Modern Web Interface** - Clean, responsive React UI
- 🎯 **Accurate Detection** - Advanced boss detection using multiple memory addresses
- ⚡ **Compact Layout** - 1px spacing between tiles for maximum efficiency
- 🔄 **Background Polling** - Efficient UDP polling with instant cache serving
- 🧪 **Full Test Coverage** - Comprehensive test suite

## Prerequisites

1. **Node.js 18+** with npm
2. **RetroArch** with network commands enabled
3. **Super Metroid ROM** loaded in RetroArch

## Quick Start

### 1. Install Dependencies
```bash
cd app/
npm install
```

### 2. Configure RetroArch
Enable network commands in RetroArch:
- Settings → Network → Network Commands → ON
- Default port: 55355

### 3. Start the Unified App
```bash
# Development mode (frontend + backend)
npm run dev:full

# Or production mode
npm run start
```

### 4. Open Browser
Navigate to `http://localhost:3000/` (frontend) or `http://localhost:8080/` (backend API)

## 🏠 Unified Server Architecture

**NEW**: The app now supports a **true unified server** approach! Instead of running separate frontend and backend servers, you can run a single Node.js server that:

✅ **Serves the React app** (built static files)  
✅ **Provides all API endpoints** (RetroArch communication)  
✅ **Handles background polling** (game state updates)  
✅ **Single port, single process** (homogeneous solution)

### Single Server Mode

```bash
# Build and run unified server (recommended for production)
npm run start

# The server will:
# 1. Build the React app (npm run build)
# 2. Start the unified server on port 8080
# 3. Serve both the web app AND the API from the same server
```

**Access everything from one URL**: `http://localhost:8080/`
- **Web App**: `http://localhost:8080/` (React interface)
- **API**: `http://localhost:8080/api/status` (JSON endpoints)
- **Game State**: `http://localhost:8080/game_state` (RetroArch data)

### Architecture Comparison

**Before (Separate Servers)**:
```
Frontend (Vite) :3000  →  Backend (Node.js) :8080  →  RetroArch :55355
```

**Now (Unified Server)**:
```
Unified Server :8080  →  RetroArch :55355
     ↓
  React App + API
```

### Benefits

- 🎯 **Single deployment** - One server to rule them all
- 🔧 **Easier setup** - No port juggling or CORS issues  
- 📦 **Production ready** - Built React app served efficiently
- 🚀 **Better performance** - No cross-origin requests
- 🏠 **Homogeneous** - Frontend + Backend in perfect harmony

## Available Scripts

### 🚀 Development
```bash
# Start both frontend and backend in development mode
npm run dev:full

# Start only frontend (Vite dev server)
npm run dev

# Start only backend (TypeScript server with hot reload)
npm run server:dev
```

### 🏗️ Production
```bash
# Build and start production server
npm run start

# Build frontend only
npm run build

# Start production server only
npm run server
```

### 🧪 Testing
```bash
# Run all tests
npm test

# Run server tests only
npm run test:server

# Run tests in watch mode
npm run test -- --watch
```

### 🔧 Utilities
```bash
# Lint code
npm run lint

# Preview production build
npm run preview
```

## Usage

1. **Start RetroArch** with Super Metroid loaded
2. **Run the unified app**: `npm run dev:full` (development) or `npm run start` (production)
3. **Open browser**: Navigate to `http://localhost:3000/`
4. **Play the game** - the tracker updates in real-time!

## API Endpoints

The TypeScript backend provides the same API as the original backends:

- `GET /` - React web interface
- `GET /api/status` - Server and RetroArch connection status
- `GET /api/stats` - Current game statistics (items, bosses, etc.)
- `GET /game_state` - Game state (alias for /api/stats)
- `POST /api/reset-cache` - Reset the game state cache
- `GET /api/reset` - Reset cache (compatibility endpoint)
- `GET /health` - Health check endpoint

## Architecture

### Unified File Structure
```
app/
├── src/
│   ├── components/          # React components
│   ├── hooks/              # React hooks
│   ├── context/            # React context providers
│   ├── types/              # TypeScript type definitions
│   ├── config/             # Configuration files
│   └── server/             # TypeScript backend
│       ├── main.ts         # Server entry point
│       ├── httpServer.ts   # Express HTTP server
│       ├── backgroundPoller.ts  # Background UDP polling
│       ├── retroArchUdpClient.ts  # RetroArch UDP client
│       ├── gameStateParser.ts     # Game state parser
│       ├── types.ts        # Server type definitions
│       └── __tests__/      # Server tests
├── public/                 # Static assets
├── dist/                   # Built frontend (after npm run build)
├── package.json           # Dependencies and scripts
├── vite.config.ts         # Vite configuration
├── vitest.config.ts       # Frontend test configuration
└── vitest.config.server.ts  # Backend test configuration
```

### Migration Details

The unified app perfectly migrates functionality from:

1. **Kotlin Backend** (`server_kotlin/`) → **TypeScript Backend** (`app/src/server/`)
   - ✅ RetroArch UDP client
   - ✅ Game state parser with exact bit patterns
   - ✅ Background polling architecture
   - ✅ HTTP server with all endpoints
   - ✅ Mother Brain detection logic
   - ✅ Boss detection algorithms

2. **React Frontend** (`app_fe_only/`) → **Unified Frontend** (`app/src/`)
   - ✅ All React components
   - ✅ Game state management
   - ✅ Real-time updates
   - ✅ Timer functionality
   - ✅ Configuration system

## Troubleshooting

### 404 Errors When Running Frontend Only

If you see 404 errors like this when running `npm run dev`:

```
Request URL: http://localhost:8080/api/status
Status Code: 404 Not Found
```

**This is because browsers cannot directly communicate with RetroArch via UDP.** The frontend needs the backend server to act as a bridge.

**Quick Fix:**
```bash
# Instead of just: npm run dev
# Use this to start both frontend and backend:
npm run dev:full
```

**Why This Happens:**
- Browsers have security restrictions that prevent direct UDP communication
- RetroArch only supports UDP connections (port 55355)
- The backend server bridges HTTP (browser) ↔ UDP (RetroArch)

**Architecture:**
```
Browser (HTTP) → Backend Server (UDP) → RetroArch
```

For more details, see `app/BROWSER_LIMITATIONS.md`.

### Port Issues
```bash
# Kill processes on port 3000 (frontend)
lsof -ti :3000 | xargs kill -9 2>/dev/null

# Kill processes on port 8080 (backend)
lsof -ti :8080 | xargs kill -9 2>/dev/null
```

### RetroArch Not Connecting
- Ensure RetroArch network commands are enabled
- Check RetroArch is using port 55355
- Verify Super Metroid is loaded and playing

### Development Issues
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Check if TypeScript compiles
npm run build
```

## Features Implemented

### ✅ Items Tracked
- Morph Ball, Bombs, Varia Suit, Gravity Suit
- Hi-Jump Boots, Speed Booster, Space Jump, Screw Attack
- Spring Ball, Grappling Beam, X-Ray Scope
- Charge Beam, Ice Beam, Wave Beam, Spazer, Plasma Beam
- Energy Tanks (always colored), Missiles, Super Missiles, Power Bombs

### ✅ Bosses Tracked  
- Bomb Torizo, Kraid, Spore Spawn, Crocomire
- Phantoon, Botwoon, Draygon, Ridley, Golden Torizo
- Mother Brain (with advanced phase detection)

### ✅ Technical Features
- **Background UDP Polling** - Efficient RetroArch communication
- **Instant Cache Serving** - Sub-millisecond API responses
- **Advanced Boss Detection** - Multiple memory address validation
- **Game State Persistence** - Timer and state management
- **Comprehensive Testing** - Full test coverage
- **Modern TypeScript** - Type-safe development

## Legacy Backends

The original backends are preserved for reference:

- `server_kotlin/` - Original Kotlin Native backend
- `server_python/` - Original Python backend
- `app_fe_only/` - Original frontend-only React app

## Credits

Built for live Super Metroid speedrun/randomizer tracking with RetroArch integration. 

**Migration completed**: Separate BE + FE → Unified TypeScript Web App ✅
