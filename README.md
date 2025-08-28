# Super Metroid Auto Tracker

A real-time item, boss, and location tracker for Super Metroid that automatically reads game state from RetroArch via UDP.

## 🎮 Overview

Super Metroid Auto Tracker is a desktop application built with Kotlin and Jetpack Compose that automatically tracks your progress through Super Metroid. It connects to RetroArch via UDP to read the game's memory in real-time, providing a visual display of:

- Items and upgrades collected
- Bosses defeated
- Current location (area and room)
- Health, missiles, and other stats
- Speedrun splits and timing

The application serves both casual players who want to keep track of their progress and speedrunners who need precise timing and split information.

## ✨ Features

### Real-Time Game State Tracking
- **Items & Upgrades**: Tracks all major items (Morph Ball, Bombs, Beams, Suits, etc.)
- **Bosses**: Monitors boss defeat status, including multi-phase bosses like Mother Brain
- **Location**: Shows current area and room names
- **Stats**: Displays health, missiles, super missiles, power bombs, and reserves

### Speedrunning Tools
- **Auto Splits**: Automatically tracks and times segments of your run
- **Split Comparison**: Compare current splits against personal bests
- **Run State Management**: Start, pause, and reset runs with keyboard shortcuts

### User Interface
- **Compact Layout**: Designed for minimal screen space usage
- **Customizable Display**: Toggle visibility of icons, splits, and timer
- **Status Grid**: Visual representation of all collected items and defeated bosses

## 🚀 Installation

### Prerequisites
- Java 11 or higher
- RetroArch with network commands enabled
- Super Metroid ROM loaded in RetroArch

### Download and Run
1. Download the latest release from the Releases page
2. Extract the archive to a location of your choice
3. Run the application:
   - **Windows**: Double-click the `.exe` file
   - **macOS**: Double-click the `.app` file
   - **Linux**: Run the `.AppImage` file or use the shell script

### RetroArch Configuration
1. In RetroArch, go to Settings → Network
2. Enable "Network Commands" (set to ON)
3. Keep the default port (55355) or note your custom port for configuration

## 🎯 Usage

### Basic Operation
1. Start RetroArch and load Super Metroid
2. Launch Super Metroid Auto Tracker
3. The application will automatically connect to RetroArch and begin tracking

### Keyboard Shortcuts
- **Spacebar**: Start/pause the timer for speedruns
- **R**: Reset the current run
- **Esc**: Exit the application

### UI Controls
- Use the toggle buttons at the bottom of the window to show/hide:
  - **icons**: Item and boss status grid
  - **splits**: Speedrun split times
  - **timer**: Run timer

## 🔍 Methodology for Tracking Items/Bosses

The application uses a sophisticated memory reading approach to track game state:

### Memory Reading Process
1. Connects to RetroArch via UDP on port 55355 (configurable)
2. Polls game memory at regular intervals (default: 500ms)
3. Parses raw memory values into structured game state
4. Applies stability checks to prevent erratic updates
5. Updates the UI with the latest verified game state

### Item Tracking
Items are tracked by reading specific memory addresses and bit flags:

- **Equipment Items**: Memory address `0x09A4-0x09A5` with the following bit flags:
  - Varia Suit: `0x0001` (Bit 0)
  - Spring Ball: `0x0002` (Bit 1)
  - Morph Ball: `0x0004` (Bit 2)
  - Screw Attack: `0x0008` (Bit 3)
  - Gravity Suit: `0x0020` (Bit 5)
  - Hi-Jump: `0x0100` (Bit 8)
  - Space Jump: `0x0200` (Bit 9)
  - Bombs: `0x1000` (Bit 12)
  - Speed Booster: `0x2000` (Bit 13)
  - Grapple: `0x4000` (Bit 14)
  - X-Ray: `0x8000` (Bit 15)

- **Beam Upgrades**: Memory address `0x09A8-0x09A9` with the following bit flags:
  - Wave Beam: `0x0001` (Bit 0)
  - Ice Beam: `0x0002` (Bit 1)
  - Spazer: `0x0004` (Bit 2)
  - Plasma: `0x0008` (Bit 3)
  - Charge Beam: `0x1000` (Bit 12)

### Boss Tracking
Bosses are tracked using area-specific boss flags in memory:

- **Crateria Bosses**: Memory address `0xD828`
  - Bomb Torizo: `0x0004` (Bit 2)

- **Brinstar Bosses**: Memory address `0xD829`
  - Spore Spawn: `0x0002` (Bit 1)
  - Kraid: `0x0001` (Bit 0)

- **Norfair Bosses**: Memory address `0xD82A`
  - Ridley: `0x0001` (Bit 0)
  - Crocomire: `0x0002` (Bit 1)
  - Golden Torizo: `0x0004` (Bit 2)

- **Wrecked Ship Bosses**: Memory address `0xD82B`
  - Phantoon: `0x0001` (Bit 0)

- **Maridia Bosses**: Memory address `0xD82C`
  - Draygon: `0x0001` (Bit 0)
  - Botwoon: `0x0002` (Bit 1)

- **Tourian Bosses**: Memory address `0xD82D`
  - Mother Brain: `0x0002` (Bit 1)

- **Ceres Bosses**: Memory address `0xD82E`
  - Ceres Ridley: `0x0001` (Bit 0)

### Special Boss Logic
Some bosses require special detection logic:

- **Mother Brain**: Tracked in phases using HP thresholds and room ID
  - Phase 1: HP >= 18000 in Mother Brain room
  - Phase 2: HP >= 36000 in Mother Brain room
  - Final: Tourian boss flag bit 1

- **Golden Four**: Detected when all four major bosses (Kraid, Phantoon, Draygon, Ridley) are defeated

- **Ship**: Detected using a combination of event flags, ship AI state, and Mother Brain defeat status

### Location Tracking
- **Area ID**: Memory address `0x079F` maps to area names:
  - 0: Crateria
  - 1: Brinstar
  - 2: Norfair
  - 3: Wrecked Ship
  - 4: Maridia
  - 5: Tourian
  - 6: Ceres Station

- **Room ID**: Memory address `0x079B` contains a unique ID for each room
  - The application maps these IDs to human-readable room names

## 🏗️ Project Structure

```
src/
├── main/kotlin/com/supermetroid/
│   ├── autosplits/        # Auto-splitting logic for speedruns
│   ├── gamestate/         # Game state parsing and memory mapping
│   ├── model/             # Data models (GameState, Items, Bosses, etc.)
│   ├── network/           # RetroArch UDP communication
│   ├── service/           # Background services for polling and state management
│   ├── storage/           # File storage for saving/loading split data
│   └── ui/                # Jetpack Compose UI components
│       ├── components/    # Reusable UI components
│       └── theme/         # UI theme and styling
```

## 🧪 Building and Testing

### Building from Source
1. Clone the repository
2. Build with Gradle:
   ```
   ./gradlew build
   ```
3. Run the application:
   ```
   ./gradlew run
   ```
4. Create a distribution package:
   ```
   ./gradlew packageDistribution
   ```

### Testing
Run the tests with:
```
./gradlew test
```

The project includes tests for:
- Auto-splits logic
- Game state parsing
- Memory reading stability

## 📚 Research and References

The memory addresses and logic used in this project are based on:
- [Super Metroid RAM Map](https://jathys.zophar.net/supermetroid/kejardon/RAMMap.txt)
- [Super Metroid AutoSplitter](https://github.com/UNHchabo/AutoSplitters)
- [SNES9x Memory Mapping](https://github.com/gocha/snes9x-rr-lua/blob/master/snes9x-rr-1.43-src/cheats2.cpp)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- The Super Metroid speedrunning community for documenting memory addresses
- RetroArch developers for providing the network interface
- JetBrains for Kotlin and Compose for Desktop
