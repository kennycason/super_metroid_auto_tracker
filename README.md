# Super Metroid Auto Tracker

A modern, component-based Super Metroid tracker built with Node.js + React frontend and TypeScript. This tracker automatically detects game state from Super Metroid ROM memory and provides real-time tracking of items, bosses, and game progress.

## 🎯 Features

- **Real-time tracking** of items, bosses, and game stats
- **Timer and splits** with automatic detection
- **Fullscreen mode** for streaming/recording
- **Responsive design** that works on all screen sizes
- **Modular components** for easy customization
- **Configuration-based** different speedrun categories
- **Live connection** to backend server with memory parsing

## 🏗️ Architecture

### Component Structure

```
src/
├── components/
│   ├── Items/           # Item tracking components
│   │   ├── Item.tsx     # Individual item component
│   │   └── ItemsGrid.tsx # Grid layout for items
│   ├── Bosses/          # Boss tracking components
│   │   ├── Boss.tsx     # Individual boss component
│   │   └── BossesGrid.tsx # Grid layout for bosses
│   ├── Timer/           # Timer and splits
│   │   ├── Timer.tsx    # Timer display and controls
│   │   └── Splits.tsx   # Splits list
│   ├── UI/              # General UI components
│   │   ├── Header.tsx   # App header with status
│   │   ├── Stats.tsx    # Player stats display
│   │   └── Location.tsx # Current area/room
│   └── Layout/          # Layout components
│       └── Tracker.tsx  # Main tracker layout
├── context/             # React Context for state management
│   └── SuperMetroidContext.tsx
├── hooks/               # Custom React hooks
│   └── useGameState.ts  # Game state fetching hook
├── types/               # TypeScript type definitions
│   ├── gameState.ts     # Game state interfaces
│   └── config.ts        # Configuration interfaces
└── config/              # Configuration files
    └── defaultConfig.ts # Default tracker configuration
```

### Key Design Patterns

1. **Context Pattern**: `SuperMetroidContext` provides game state to all components
2. **Custom Hooks**: `useGameState` handles backend communication and state management
3. **Configuration-Driven**: JSON configs define which items/bosses to show and layouts
4. **Component Composition**: Small, focused components that can be reused
5. **TypeScript First**: Full type safety throughout the application

## 🚀 Getting Started

### Prerequisites

- Node.js 20+ (currently using 22.0.0)
- **RetroArch with bsnes core** (or standalone bsnes)
- **Network Commands (NWA) enabled** in RetroArch settings
- Super Metroid ROM loaded in emulator
- Backend server running on `localhost:8000`

### Setup

1. **Configure RetroArch/bsnes:**
   - Enable Network Commands (NWA) in RetroArch settings
   - Load Super Metroid ROM
   - Ensure emulator is running and game is loaded

2. **Start the tracker:**

```bash
# Install dependencies
npm install

# Start full development server (frontend + backend)
npm run dev:full
```

The app will start on `http://localhost:3000`

### Production Build

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

## 🔧 Configuration

The tracker uses JSON configuration files to define:

- **Items**: Which items to track, their positions, and categories
- **Bosses**: Which bosses to track and their layout
- **Layout**: How components are arranged on screen
- **Settings**: Feature toggles and UI preferences

### Configuration Updates

If you need to modify tracking behavior:

1. **Item/Boss Layout**: Edit configuration files in `src/config/`
2. **Memory Addresses**: Update `GameStateParser.kt` if memory layout changes
3. **Polling Rate**: Adjust backend polling frequency (default: 1 second)
4. **Reset Detection**: Modify reset logic in parser if needed for different ROM versions

### Example Configuration

```typescript
{
  id: 'any-percent',
  name: 'Any% Items Only',
  items: [
    { id: 'morph', name: 'Morph Ball', enabled: true, row: 0, col: 0 },
    { id: 'bombs', name: 'Bombs', enabled: true, row: 0, col: 1 },
    // ... more items
  ],
  bosses: [
    { id: 'kraid', name: 'Kraid', enabled: true, row: 0, col: 0 },
    // ... more bosses
  ],
  settings: {
    showStats: true,
    showTimer: true,
    showSplits: true,
    // ... more settings
  }
}
```

## 🎮 Usage

### Quick Start (RetroArch - Default)

1. **Configure RetroArch/bsnes** with NWA enabled and Super Metroid ROM loaded
2. **Start the full tracker** with `npm run dev:full`
3. **Open your browser** to `http://localhost:3000`
4. **Begin playing** - the tracker will automatically detect game state
5. **Start tracking** your speedrun!

### Backend Selection Commands

The tracker supports multiple emulator backends. Here are the commands for each:

#### RetroArch Backend (Default)
```bash
# Full stack (frontend + backend) - RetroArch
npm run dev:full

# Backend only
npm run server:dev

# With custom port and poll interval
npm run server:dev -- 8000 1000

# With explicit RetroArch backend
npm run server:dev -- --backend=retroarch
```

#### Mesen Backend
```bash
# Full stack (frontend + backend) - Mesen
npm run dev:full:mesen

# Backend only with Mesen
npm run server:dev -- --backend=mesen

# With custom port and Mesen backend
npm run server:dev -- 8000 1000 --backend=mesen

# With custom Mesen host/port
npm run server:dev -- --backend=mesen --mesen-host=localhost --mesen-port=8080
```

#### Full Stack with Different Backends

**RetroArch (Default):**
```bash
# Single command (recommended)
npm run dev:full

# OR manually in separate terminals:
# Terminal 1: Start frontend
npm run dev
# Terminal 2: Start backend with RetroArch
npm run server:dev
```

**Mesen:**
```bash
# Single command (recommended)
npm run dev:full:mesen

# OR manually in separate terminals:
# Terminal 1: Start frontend  
npm run dev
# Terminal 2: Start backend with Mesen
npm run server:dev -- --backend=mesen
```

### Backend Configuration

#### RetroArch Requirements:
- RetroArch with bsnes core
- Network Commands (NWA) enabled
- Default: `localhost:55355` (UDP)

#### Mesen Requirements:
- Mesen 2.1.1 with HTTP API enabled
- Default: `localhost:8080` (HTTP)

### Command Line Arguments

The server accepts these arguments:
- **Positional**: `[port] [pollInterval]` (e.g., `8000 1000`)
- **Backend**: `--backend=retroarch|mesen`
- **RetroArch**: `--retroarch-host=HOST --retroarch-port=PORT`
- **Mesen**: `--mesen-host=HOST --mesen-port=PORT`

### Controls

- **Timer**: Start/Stop/Reset buttons in the timer section
- **Fullscreen**: Click the ⛶ button in the header
- **Connection Status**: Shows in the header (green = connected)

## 🔄 Backend Integration

The React app connects to the existing Python backend server:

- **Endpoint**: `http://localhost:8000/game_state`
- **Polling**: Every 1 second for real-time updates
- **Format**: JSON with all game state information

### Data Flow

```
Emulator → UDP → Python Backend → HTTP API → React Frontend
```

## 🎨 Styling

- **CSS Modules**: Component-scoped styling
- **Dark Theme**: Retro green terminal aesthetic
- **Responsive**: Mobile-first design
- **Pixelated Graphics**: Crisp sprite rendering

## 🧠 Memory Addresses & Game State Parsing

The tracker reads Super Metroid's memory to automatically detect game state. Here are the key memory addresses and parsing rules:

### Core Memory Map

| Address | Description |
|---------|-------------|
| `0x7E09C2` | Current Health |
| `0x7E09C4` | Max Health |
| `0x7E09C6` | Current Missiles |
| `0x7E09C8` | Max Missiles |
| `0x7E09CA` | Current Super Missiles |
| `0x7E09CC` | Max Super Missiles |
| `0x7E09CE` | Current Power Bombs |
| `0x7E09D0` | Max Power Bombs |
| `0x7E09D6` | Reserve Energy |
| `0x7E09D4` | Max Reserve Energy |
| `0x7E079B` | Room ID |
| `0x7E079F` | Area ID |
| `0x7E0998` | Game State |
| `0x7E0AF6` | Player X Position |
| `0x7E0AFA` | Player Y Position |
| `0x7E09A4` | Items Bitfield |
| `0x7E09A8` | Beams Bitfield |
| `0x7ED828` | Bosses Bitfield |
| `0x7E0FB2` | Ship AI State |
| `0x7ED821` | Event Flags |

### Items Parsing (Address: 0x7E09A4)

| Item | Bit Flag | Description |
|------|----------|-------------|
| Morph Ball | `0x0004` | Allows rolling into ball form |
| Bombs | `0x1000` | Morph ball bombs |
| Varia Suit | `0x0001` | Heat protection suit |
| Gravity Suit | `0x0020` | Water/lava protection |
| Hi-Jump Boots | `0x0100` | Higher jumping ability |
| Speed Booster | `0x2000` | Running speed boost |
| Space Jump | `0x0200` | Infinite spin jumping |
| Screw Attack | `0x0008` | Damaging spin jump |
| Spring Ball | `0x0002` | Jump while in morph ball |
| Grappling Beam | `0x4000` | Swing from grapple points |
| X-Ray Scope | `0x8000` | See hidden blocks |

### Beams Parsing (Address: 0x7E09A8)

| Beam | Bit Flag | Description |
|------|----------|-------------|
| Charge Beam | `0x1000` | Charge shots for more damage |
| Ice Beam | `0x0002` | Freeze enemies |
| Wave Beam | `0x0001` | Pass through walls |
| Spazer | `0x0004` | Split beam shots |
| Plasma Beam | `0x0008` | High damage beam |
| Hyper Beam | `0x0010` | Final beam (Mother Brain) |

### Bosses Parsing

| Boss                  | Detection Method | Description |
|-----------------------|------------------|-------------|
| Bomb Torizo           | Main bosses bit `0x0004` | Tutorial boss |
| Kraid                 | Main bosses bit `0x0100` | Brinstar boss |
| Spore Spawn           | Main bosses bit `0x0200` | Brinstar mini-boss |
| Mother Brain          | Main bosses bit `0x0001` | Final boss |
| Crocomire             | Special logic: `(value & 0x0002) != 0 && value >= 0x0202` | Norfair boss |
| Phantoon              | Boss Plus 3: `(value & 0x01) != 0` | Wrecked Ship boss |
| Botwoon               | Complex detection using Boss Plus 2 & 4 | Maridia mini-boss |
| Draygon               | Boss Plus 3: `value == 0x0301` | Maridia boss |
| Ridley                | Complex detection using Boss Plus 2 & 4 | Norfair boss |
| Golden Torizo         | Multiple condition detection | Norfair mini-boss |
| Mother Brain 1 (TODO) | In MB room + MB bit set | First phase |
| Mother Brain 2 (TODO) | MB1 detected + escape timer active | Second phase |
| Samus Ship (TODO)     | Ship AI `0xaa4f` + Event flags `0x40` | End game completion |

### Area IDs

| ID | Area Name |
|----|-----------|
| 0 | Crateria |
| 1 | Brinstar |
| 2 | Norfair |
| 3 | Wrecked Ship |
| 4 | Maridia |
| 5 | Tourian |

### Reset Detection Rules

The tracker automatically detects new games and resets item/boss states when:

1. **Intro Scene**: Area 0 (Crateria) + Health ≤ 99 + Room ID < 1000
2. **New Game**: Health = 99 + Missiles = 0 + Max Missiles = 0 + Room ID < 1000

### Ship Detection (End Game)

Multiple detection methods for game completion:

1. **Official**: Ship AI = `0xaa4f` + Event flags bit `0x40` (Zebes ablaze)
2. **Position-based**: Specific room IDs (31224, 37368) + player coordinates (1150-1350 X, 1080-1380 Y)
3. **Emergency**: MB2 complete + valid area + ship coordinates
