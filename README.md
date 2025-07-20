# Super Metroid Live Tracker

A real-time Super Metroid item and boss tracker that reads game memory from RetroArch via UDP.

## Features

- 🎮 **Live Game Tracking** - Real-time item and boss status
- 🔧 **RetroArch Integration** - Connects via UDP to read game memory  
- 🌐 **Web Interface** - Clean, responsive UI at localhost:3000
- 🎯 **Accurate Detection** - Advanced boss detection using multiple memory addresses
- ⚡ **Compact Layout** - 1px spacing between tiles for maximum efficiency

## Prerequisites

1. **Python 3.13+** with virtual environment
2. **RetroArch** with network commands enabled
3. **Super Metroid ROM** loaded in RetroArch

## Setup

### 1. Create Virtual Environment
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

### 2. Install Dependencies
```bash
pip install flask requests
```

### 3. Configure RetroArch
Enable network commands in RetroArch:
- Settings → Network → Network Commands → ON
- Default port: 55355

## Server Management Commands

### 🚀 Start Server (Recommended)
```bash
# Start the tracker (includes both UDP tracker and web server)
source venv/bin/activate && python tracker_web_server.py
```

### 🔄 Restart Server (Clean)
```bash
# Kill all tracker processes and restart
pkill -f "tracker_web_server.py\|super_metroid_udp_tracker.py"; lsof -ti :3000 | xargs kill -9 2>/dev/null; sleep 3 && source venv/bin/activate && python tracker_web_server.py > web_server.log 2>&1 &
```

### ⏹️ Stop Server
```bash
# Stop all tracker processes
pkill -f "tracker_web_server.py\|super_metroid_udp_tracker.py"
```

### 🧹 Force Kill Everything
```bash
# Nuclear option - kill everything using port 3000
pkill -f "tracker_web_server.py\|super_metroid_udp_tracker.py"; lsof -ti :3000 | xargs kill -9 2>/dev/null
```

### 📊 Check Status
```bash
# Check if servers are running
ps aux | grep -E "(tracker_web_server|super_metroid_udp_tracker)" | grep -v grep

# Test API response
curl -s http://localhost:3000/api/stats | jq '.bosses.ridley, .bosses.golden_torizo'

# Check web interface
curl -s http://localhost:3000/ | head -10
```

### 🔍 View Logs
```bash
# View web server logs
tail -f web_server.log

# Test UDP tracker directly (debug mode)
source venv/bin/activate && python super_metroid_udp_tracker.py
```

## Usage

1. **Start RetroArch** with Super Metroid loaded
2. **Run the tracker**: `source venv/bin/activate && python tracker_web_server.py`
3. **Open browser**: Navigate to `http://localhost:3000/`
4. **Play the game** - the tracker updates in real-time!

## Troubleshooting

### Port 3000 Already in Use
```bash
# Kill whatever is using port 3000
lsof -ti :3000 | xargs kill -9 2>/dev/null
```

### RetroArch Not Connecting
- Ensure RetroArch network commands are enabled
- Check RetroArch is using port 55355
- Verify Super Metroid is loaded and playing

### Tracker Not Updating
- Refresh the web page
- Check RetroArch connection
- Restart the tracker server

## API Endpoints

- `GET /` - Web interface
- `GET /api/status` - Server and RetroArch connection status
- `GET /api/stats` - Current game statistics (items, bosses, etc.)

## File Structure

```
super_metroid_hud/
├── tracker_web_server.py      # Main web server + UDP tracker
├── super_metroid_udp_tracker.py  # UDP tracker (can run standalone)
├── super_metroid_tracker.html    # Web interface
├── item_sprites.png              # Item sprite sheet
├── boss_sprites.png              # Boss sprite sheet
└── README.md                     # This file
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
- Mother Brain

### ✅ Recent Fixes
- **Ridley & Golden Torizo Detection**: Using exact logic from original working tracker
- **Energy Tanks**: Always show as obtained (never grayed out)
- **Tile Spacing**: Reduced to 1px for compact layout
- **Debug Spam**: Eliminated console flooding

## Credits

Built for live Super Metroid speedrun/randomizer tracking with RetroArch integration. 