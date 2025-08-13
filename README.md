# Super Metroid Tracker (React)

A modern, component-based Super Metroid tracker built with React and TypeScript. This is the next-generation version of the tracker, designed to be modular, extensible, and maintainable.

## 🎯 Features

- **Real-time tracking** of items, bosses, and game stats
- **Timer and splits** with automatic detection
- **Fullscreen mode** for streaming/recording
- **Responsive design** that works on all screen sizes
- **Modular components** for easy customization
- **Configuration-based** different speedrun categories
- **Live connection** to existing backend server

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
- Backend server running on `localhost:8000`

### Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev
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

1. **Start the backend server** (Python-based) on port 8000
2. **Start the React app** with `npm run dev`
3. **Open your browser** to `http://localhost:3000`
4. **Connect your emulator** using the existing UDP connection
5. **Start tracking** your speedrun!

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

## 🧪 Future Extensions

The modular design allows for easy additions:

- **New Speedrun Categories**: Add config files for Low%, 100%, etc.
- **Custom Layouts**: Different arrangements for different use cases
- **Additional Tracking**: Room transitions, route optimization
- **Themes**: Light mode, custom color schemes
- **Integrations**: Twitch, LiveSplit, etc.

## 🔧 Development Notes

- **Legacy Compatibility**: The original `super_metroid_tracker.html` remains untouched
- **Type Safety**: Full TypeScript coverage for reliability
- **Modern React**: Uses functional components, hooks, and context
- **Performance**: Optimized polling and minimal re-renders
- **Accessibility**: Keyboard navigation and screen reader support

## 📝 License

Same as the original tracker project.
