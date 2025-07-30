# Super Metroid Tracker - Kotlin Native Backend

A **Kotlin Native** backend server that replicates the functionality of the Python backend using **Ktor** for HTTP serving and **coroutines** for background polling.

## 🎯 Overview

This is a complete reimplementation of the Super Metroid tracker backend in Kotlin Native, providing:

- **🚀 Native Performance**: Compiled to native binary, no JVM overhead
- **⚡ Async/Await**: Modern coroutines-based architecture
- **🔄 Background Polling**: Continuous game state polling with caching
- **🌐 HTTP API**: Ktor-based REST API compatible with existing frontend
- **🧪 Comprehensive Tests**: Unit tests for core functionality
- **🔧 Memory Safety**: Type-safe memory parsing and UDP communication

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   RetroArch     │◄───┤ RetroArchUDP     │◄───┤ BackgroundPoller│
│   (Game Memory) │    │ Client           │    │ (Caching)       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   React Frontend│◄───┤ Ktor HTTP        │◄───┤ GameStateParser │
│   (Port 3000)   │    │ Server (8081     │    │ (Memory Parsing)│
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 📁 Project Structure

```
server/
├── src/
│   ├── main/kotlin/com/supermetroid/
│   │   ├── model/             # Data models (GameState, ServerStatus, etc.)
│   │   ├── client/            # RetroArch UDP client
│   │   ├── parser/            # Game memory parsing logic
│   │   ├── service/           # Background polling service
│   │   ├── server/            # Ktor HTTP server
│   │   └── Main.kt            # Application entry point
│   └── test/kotlin/com/supermetroid/
│       ├── model/             # Data model tests
│       ├── parser/            # Parser logic tests
│       └── service/           # Service tests
├── build.gradle.kts           # Kotlin Native build configuration
├── gradle.properties         # Gradle settings
└── README.md                 # This file
```

## 🚀 Getting Started

### Prerequisites

- **Kotlin/Native 1.9.21+**
- **Gradle 8.5+**
- **RetroArch** with network commands enabled
- **Super Metroid ROM** loaded in RetroArch

### Build & Run

```bash
# Navigate to server directory
cd server/

# Build the native binary
./gradlew build

# Run the server
./gradlew runDebugExecutableNative

# Or run with custom port and poll interval
./gradlew runDebugExecutableNative --args="8081 1000"
```

### Development Mode

```bash
# Run tests
./gradlew test

# Build and run in one command
./gradlew clean build runDebugExecutableNative

# Build release binary
./gradlew linkReleaseExecutableNative
```

## 🌐 API Endpoints

The Kotlin Native server provides the same API endpoints as the Python backend:

### Core Endpoints
- `GET /` - Server status page
- `GET /api/status` - Complete server and game status
- `GET /api/stats` - Game statistics only
- `GET /game_state` - Game state (React app format)

### Control Endpoints  
- `GET /api/reset-cache` - Reset all caches
- `GET /api/reset-mb-cache` - Reset Mother Brain cache only
- `GET /api/manual-mb-complete` - Manually mark MB complete
- `GET /api/bootstrap-mb` - Bootstrap MB cache
- `GET /health` - Health check endpoint

### Example Response

```json
{
  "connected": true,
  "gameLoaded": true,
  "retroarchVersion": "1.15.0",
  "gameInfo": "Super Metroid",
  "stats": {
    "health": 299,
    "maxHealth": 399,
    "missiles": 50,
    "maxMissiles": 230,
    "areaId": 1,
    "areaName": "Brinstar",
    "items": {
      "morph": true,
      "bombs": true,
      "varia": false
    },
    "beams": {
      "charge": true,
      "ice": true,
      "wave": false
    },
    "bosses": {
      "kraid": true,
      "ridley": false
    }
  },
  "lastUpdate": 1640995200000,
  "pollCount": 150,
  "errorCount": 0
}
```

## 🧪 Testing

### Run All Tests
```bash
./gradlew test
```

### Test Coverage

- **Parser Tests**: Memory parsing logic, item/beam/boss detection
- **Model Tests**: Data serialization and structure validation  
- **Service Tests**: Background polling and caching logic
- **Integration Tests**: End-to-end API functionality

### Test Example
```kotlin
@Test
fun testParseItemsNewGame() = runTest {
    val gameState = parser.parseCompleteGameState(newGameMemoryData)
    
    // Should reset all items in new game
    assertFalse(gameState.items["morph"] ?: true)
    assertFalse(gameState.items["charge"] ?: true)
}
```

## 🔧 Configuration

### Command Line Arguments
```bash
# Port (default: 8080)
./binary 8080

# Port + Poll Interval in ms (default: 1000ms)  
./binary 8080 1500
```

### RetroArch Setup
Ensure RetroArch has network commands enabled:
- Settings → Network → Network Commands → ON
- Default port: 55355

## 🎮 Features Implemented

### ✅ Core Functionality
- **Background UDP Polling** - Continuous RetroArch memory reading
- **Memory Parsing** - Complete Super Metroid game state extraction
- **Caching System** - Thread-safe cached responses
- **HTTP API** - Full REST API compatibility
- **CORS Support** - Cross-origin requests for React frontend

### ✅ Game State Tracking
- **Items**: Morph Ball, Bombs, Varia, Gravity, Beams, etc.
- **Bosses**: Kraid, Ridley, Mother Brain phases, etc.
- **Stats**: Health, Missiles, Supers, Power Bombs
- **Location**: Area, Room, Player position

### ✅ Advanced Features  
- **Smart Reset Detection** - Prevents cache bugs during new games
- **Mother Brain Phases** - Advanced boss detection logic
- **Bootstrap Logic** - Intelligent cache initialization
- **Error Handling** - Graceful failure and recovery

## 🔄 Migration from Python

This Kotlin Native server is designed as a **drop-in replacement** for the Python backend:

1. **Same API endpoints** - No frontend changes required
2. **Same JSON responses** - Compatible data formats
3. **Same functionality** - All features preserved
4. **Better performance** - Native compilation benefits
5. **Type safety** - Compile-time error detection

### Switching Backends

```bash
# Stop Python server
pkill -f background_poller_server.py

# Start Kotlin Native server  
cd server && ./gradlew runDebugExecutableNative

# Or run both on different ports for comparison
# Python: http://localhost:8000
# Kotlin: http://localhost:8080
```

## 🚀 Performance Benefits

- **🏃 Native Speed**: No JVM startup time or garbage collection pauses
- **💾 Memory Efficient**: Lower memory footprint than JVM
- **⚡ Fast Startup**: Near-instantaneous server startup
- **🔄 Async I/O**: Coroutines-based non-blocking architecture
- **🎯 Zero Dependencies**: Self-contained native binary

## 🛠️ Development

### Adding New Features

1. **Models**: Add data classes in `model/`
2. **Parsing**: Extend `GameStateParser` for new memory locations
3. **API**: Add endpoints in `server/HttpServer.kt`
4. **Tests**: Create tests in `test/` directory

### Code Style

- **Kotlin conventions**: Official Kotlin code style
- **Coroutines**: Suspend functions for async operations
- **Type safety**: Leverage Kotlin's type system
- **Documentation**: KDoc comments for public APIs

## 📝 License

Same license as the original Super Metroid Tracker project. 
