# Controller Tracker Feature

The Controller Tracker is a new feature that displays real-time gamepad input as colored rectangles, showing which buttons are currently pressed and tracking button press frequency for visual feedback.

## Features

### Visual Button Display
- **12 Button Layout**: Displays buttons from left to right: D-pad Left, D-pad Up, D-pad Right, D-pad Down, Select, Start, L, R, X, Y, B, A
- **Color-Coded Buttons**: Different colors for different button types:
  - 🔵 **Blue**: D-pad buttons (Left, Up, Right, Down)
  - 🟣 **Purple**: System buttons (Select, Start)
  - 🟢 **Green**: Shoulder buttons (L, R)
  - 🟡 **Gold**: Face buttons top row (X, Y)
  - 🔴 **Red**: Face buttons bottom row (B, A)

### Real-Time Feedback
- **Active State**: Buttons light up with full color when pressed
- **Inactive State**: Buttons show dimmed color when not pressed
- **Border Highlighting**: Pressed buttons get thicker, more prominent borders

### Frequency Tracking & Glow Effects
- **Press Frequency**: Tracks how many times per second each button is pressed
- **Glow Animation**: High-frequency buttons (>1 press/second) get a pulsing glow effect
- **Intensity Scaling**: Glow intensity increases with press frequency (max at 10 presses/second)
- **Frequency Indicator**: Small colored dots appear below buttons with high activity
- **Decay System**: Frequency gradually decreases when buttons aren't pressed

## Usage

### Enabling the Controller Tracker
1. Click the **"ctl"** toggle button in the bottom control panel
2. The controller tracker panel will appear between the timer and splits sections
3. The panel matches the timer's height for consistent layout

### Controller Support
The system is designed to support multiple controller types:
- **SNES Controllers**
- **Nintendo Switch Pro Controllers** 
- **PlayStation 4/5 Controllers**
- **Xbox Controllers**
- **8BitDo Controllers**
- **Generic USB Controllers** (fallback mapping)

### Current Implementation
- **Simulation Mode**: Currently runs in simulation mode for testing
- **60 FPS Polling**: Updates at ~60 FPS for smooth visual feedback
- **Automatic Detection**: Attempts to detect and configure connected controllers

## Technical Details

### Architecture
- **ControllerService**: Handles gamepad input detection and button state tracking
- **ControllerTrackerPanel**: UI component rendering the visual feedback
- **Frequency Tracking**: Real-time calculation of button press rates with smoothing

### Button Mapping
The system uses a flexible configuration system that maps physical gamepad buttons to logical SNES-style button names:

```kotlin
// Example SNES controller mapping
buttonMapping = mapOf(
    "select" to listOf(8),
    "start" to listOf(9),
    "l" to listOf(4),
    "r" to listOf(5),
    "x" to listOf(2),
    "y" to listOf(3),
    "b" to listOf(0),
    "a" to listOf(1)
)
```

### Performance
- **Lightweight**: Minimal CPU overhead with efficient state tracking
- **Smooth Animation**: Uses Compose animations for fluid visual effects
- **Memory Efficient**: Bounded frequency tracking with automatic cleanup

## Future Enhancements

### Real Gamepad Integration
- Integration with Java gamepad APIs (JInput or similar)
- Automatic controller detection and configuration
- Support for multiple simultaneous controllers

### Advanced Features
- **Input Recording**: Record and playback button sequences
- **Macro Detection**: Identify repeated input patterns
- **Customizable Colors**: User-configurable color schemes
- **Layout Options**: Alternative button layouts (arcade stick, etc.)

### Accessibility
- **High Contrast Mode**: Enhanced visibility options
- **Size Scaling**: Adjustable button size for better visibility
- **Audio Feedback**: Optional sound cues for button presses

## Configuration

### Enabling/Disabling
The controller tracker can be toggled on/off using:
- The **"ctl"** button in the main UI controls
- Keyboard shortcuts (future enhancement)
- Configuration file settings (future enhancement)

### Visual Customization
Currently uses the app's theme colors, with plans for:
- Custom color schemes
- Adjustable glow intensity
- Configurable animation speeds
- Layout customization options

## Troubleshooting

### No Controller Detected
- Ensure your gamepad is properly connected
- Check that the controller is recognized by your operating system
- Try disconnecting and reconnecting the controller

### Simulation Mode
- The current implementation runs in simulation mode for testing
- Simulated input cycles through different buttons every few seconds
- This will be replaced with real gamepad input in future versions

### Performance Issues
- If you experience lag, try reducing the polling rate
- Disable the controller tracker if not needed to save resources
- Check for conflicting gamepad software

## Integration with Super Metroid Tracker

The controller tracker integrates seamlessly with the existing Super Metroid Auto Tracker:
- **Same Height as Timer**: Maintains consistent UI layout
- **Theme Integration**: Uses the app's color scheme and styling
- **Service Architecture**: Follows the same pattern as other tracker services
- **Memory Efficient**: Designed to not interfere with game state tracking
