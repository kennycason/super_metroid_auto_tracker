# QUsb2Snes Test Clients & Connection Troubleshooting

## 🎯 Problem Summary

You have QUsb2Snes working with RetroArch, but Funtoon.party is experiencing connection drops with the error:
```
Error listing devices connection killed
Error attaching to device. Retrying. connection killed
```

The connection **partially works** - you can see:
- Device info is being retrieved successfully
- RetroArch is detected and showing game info (`Super Metroid (Japan)`)
- But the websocket connection keeps dropping

## 🔧 What We Built

I created two test clients to help you debug the connection and potentially build your own interface:

### 1. **HTML/JavaScript Test Client** (`test_client.html`)
- **Purpose**: Tests the exact same websocket connection that Funtoon uses
- **Runs in**: Any modern web browser
- **Features**:
  - Real-time connection status
  - Quick test buttons (Device List, Attach, Info, Read Memory)
  - Custom command input
  - Detailed logging of all websocket traffic
  - Handles connection drops gracefully

### 2. **Python Test Client** (`test_client.py`)
- **Purpose**: Command-line testing and more robust connection handling
- **Requires**: `pip install websocket-client`
- **Features**:
  - Automated test sequence
  - Detailed debug output
  - Connection stability testing
  - Easy to modify for your own projects

## 🚀 How to Use

### HTML Client
1. Open `test_client.html` in your web browser
2. Click "Connect" (defaults to `ws://localhost:23074`)
3. Run the quick tests to see if your connection is stable
4. Watch the log for any connection drops

### Python Client
```bash
# Install dependency
pip3 install websocket-client

# Run the test
python3 test_client.py
```

## 📊 Expected Results

### ✅ Working Connection Should Show:
```
✅ Connected to QUsb2Snes!
📨 Received: {"Results": ["RetroArch Localhost"]}
🎮 Found RetroArch device!
📨 Received: {"Results": ["1.21.0", "RetroArch", "Super Metroid (Japan", "NO_ROM_WRITE", "NO_CONTROL_CMD", "NO_FILE_CMD"]}
📋 Device Info:
   Version: 1.21.0
   Type: RetroArch
   ROM: Super Metroid (Japan
   Flags: NO_ROM_WRITE, NO_CONTROL_CMD, NO_FILE_CMD
```

### ❌ Connection Issues Show:
```
🔌 Connection closed. Code: 1006, Reason: 
❌ WebSocket error: Connection closed abnormally
```

## 🔍 Your Current Setup Status

Based on your logs, you have:
- ✅ QUsb2Snes running on ports 8080 and 23074
- ✅ RetroArch detected with network commands enabled
- ✅ Super Metroid loaded and recognized
- ✅ Device info responding correctly
- ❌ Websocket connections dropping unexpectedly

## 🛠 Troubleshooting Steps

### 1. Test Connection Stability
Run the HTML client and leave it connected for several minutes. Check if:
- Connection drops happen at regular intervals
- Specific commands trigger disconnections
- Multiple rapid commands cause issues

### 2. Check RetroArch Settings
Ensure in `retroarch.cfg`:
```ini
network_cmd_enable = "true"
network_cmd_port = "55355"  # Default port
```

### 3. Check QUsb2Snes Logs
Monitor: `QUsb2Snes.app/Contents/MacOS/log.txt` for error messages during connection drops.

### 4. Try Alternative Clients
Test with other known working clients:
- **[ALTTP Randomizer Tracker](https://samus.link)** - Very stable
- **QFile2Snes** (included): `./QFile2Snes/QFile2Snes.app/Contents/MacOS/QFile2Snes`

## 🏗 Building Your Own Client

### WebSocket Protocol
QUsb2Snes uses a simple JSON-over-WebSocket protocol:

```javascript
// Connect
const ws = new WebSocket('ws://localhost:23074');

// Send command
ws.send(JSON.stringify({
    "Opcode": "DeviceList",
    "Space": "SNES"
}));

// Receive response
ws.onmessage = (event) => {
    const response = JSON.parse(event.data);
    console.log(response.Results);
};
```

### Common Commands
```json
// Get devices
{"Opcode": "DeviceList", "Space": "SNES"}

// Attach to device
{"Opcode": "Attach", "Space": "SNES", "Operands": ["RetroArch Localhost"]}

// Get device info
{"Opcode": "Info", "Space": "SNES"}

// Read memory (address in hex, size in decimal)
{"Opcode": "GetAddress", "Space": "SNES", "Operands": ["7E0000", "16"]}

// Write memory (send command, then binary data)
{"Opcode": "PutAddress", "Space": "SNES", "Operands": ["7E0000", "4"]}
// Then send 4 bytes of binary data
```

### Memory Map (Super Metroid)
```
$7E0000-$7FFFFF  - RAM (Work RAM)
$7F0000-$7FFFFF  - Extended RAM
$F50000-$F6FFFF  - SRAM (Save data)
```

## 🎮 Useful Memory Addresses (Super Metroid)

```javascript
// Player stats
const playerHealth = "7E09C2";      // Current health (2 bytes)
const playerMaxHealth = "7E09C4";   // Max health (2 bytes)
const playerMissiles = "7E09C6";    // Current missiles (2 bytes)
const playerMaxMissiles = "7E09C8"; // Max missiles (2 bytes)

// Game state
const gameState = "7E0998";         // Game state (2 bytes)
const roomID = "7E079B";            // Current room ID (2 bytes)
const areaID = "7E079F";            // Current area ID (1 byte)

// Position
const playerX = "7E0AF6";           // Player X position (2 bytes)
const playerY = "7E0AFA";           // Player Y position (2 bytes)
```

## 📁 Project Structure
```
QUsb2Snes_TestClients/
├── test_client.html          # Web-based test client
├── test_client.py           # Python command-line client
├── QUsb2Snes_TestClients_README.md  # This file
└── examples/
    ├── basic_tracker.html   # Example: Simple stat tracker
    ├── memory_viewer.py     # Example: Memory dump tool
    └── live_stats.js        # Example: Real-time game stats
```

## 🔗 Useful Resources

- **[QUsb2Snes Documentation](https://skarsnik.github.io/QUsb2snes/)**
- **[Protocol Documentation](https://skarsnik.github.io/QUsb2snes/Protocol)**
- **[RetroArch Network Commands](https://docs.libretro.com/development/retroarch/network-control-interface/)**
- **[Super Metroid RAM Map](https://jathys.zophar.net/supermetroid/kejardon/RAMMap.txt)**

## 🎯 Next Steps

1. **Test connection stability** with the provided clients
2. **Identify specific trigger** for disconnections
3. **Consider building a local client** if web-based ones are unreliable
4. **Monitor QUsb2Snes logs** during connection drops
5. **Try different browsers/environments** for web clients

## 💡 Pro Tips

- **Use binary data carefully**: Memory read/write operations use binary WebSocket messages
- **Handle disconnections gracefully**: Always implement reconnection logic
- **Respect the protocol**: Send one command at a time, wait for responses
- **Check device flags**: RetroArch has limitations (NO_ROM_WRITE, NO_CONTROL_CMD, NO_FILE_CMD)
- **Test with different cores**: bsnes-mercury cores work better than Snes9x

## 🐛 Common Issues

1. **Port conflicts**: Make sure nothing else uses ports 8080/23074
2. **CORS issues**: Web clients need to run from `localhost` or trusted origins
3. **RetroArch timing**: Some cores handle network commands better than others
4. **Memory alignment**: Always read/write on proper boundaries
5. **Connection limits**: QUsb2Snes may limit concurrent connections

---

*Created for debugging QUsb2Snes connection issues with Funtoon.party and building custom SNES memory access tools.* 