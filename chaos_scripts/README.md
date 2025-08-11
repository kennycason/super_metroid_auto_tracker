# 🌈 Super Metroid Chaos Scripts

Welcome to the **Super Metroid Chaos Collection**! This directory contains all the scripts for creating visual chaos, memory manipulation, and fun modifications for Super Metroid through RetroArch's network control interface.

## 🎮 Prerequisites

- **RetroArch** with network control enabled (UDP port 55355)
- **Super Metroid ROM** loaded in RetroArch
- **Python 3.6+** with no additional dependencies required

## 🚀 Main Chaos Scripts

### **🎨 super_metroid_persistent_chaos.py** ⭐ **RECOMMENDED**
The ultimate chaos experience with persistent visual effects that survive room changes!

**Features:**
- 🔄 **Persistent palette changes** across room transitions
- 🖼️ **Sprite pixel corruption** with valid color indices
- 🔮 **Psychedelic mode** for maximum visual chaos
- 🌈 **Rainbow color cycling**
- 💀 **Insane mode** with super-aggressive persistence

```bash
# Basic chaos with both palette and sprite changes
python3 super_metroid_persistent_chaos.py

# Insane mode - maximum chaos with aggressive persistence
python3 super_metroid_persistent_chaos.py --insane

# Psychedelic palette-only mode
python3 super_metroid_persistent_chaos.py --palette-only --psychedelic

# Fast rainbow sprite corruption
python3 super_metroid_persistent_chaos.py --sprites-only --rainbow --speed 3
```

**Command Line Options:**
- `--insane` - Maximum chaos mode (8x speed, psychedelic, 2000 changes)
- `--psychedelic` - High contrast, dramatic color changes  
- `--rainbow` - Smooth color cycling effects
- `--turbo` - 4x speed multiplier
- `--palette-only` - Only change colors (persistent)
- `--sprites-only` - Only corrupt sprite pixels
- `--speed N` - Custom speed multiplier (0.1-20.0)
- `--max N` - Maximum number of changes

---

### **🎨 super_metroid_map_color_chaos.py**
Focused script that only changes map screen colors for safe visual effects.

```bash
# Gentle map color changes
python3 super_metroid_map_color_chaos.py

# Psychedelic map colors
python3 super_metroid_map_color_chaos.py --psychedelic --speed 2
```

---

### **⚡ super_metroid_chaos.py**
Original chaos script with broader memory corruption including map data.

```bash
# Basic chaos with map corruption
python3 super_metroid_chaos.py

# Intense corruption mode
python3 super_metroid_chaos.py --speed 5 --max 1000
```

## 🎯 Memory Manipulation Scripts

### **🔗 give_grappling_beam.py**
Instantly give Samus the grappling beam by writing to game memory.

```bash
python3 give_grappling_beam.py
```

### **🚀 give_super_missiles.py**
Add super missiles to Samus's inventory.

```bash
python3 give_super_missiles.py
```

## 🔍 Utility Scripts

### **📍 find_room_id.py**
Diagnostic tool to find the correct room ID memory address for your ROM version.

```bash
python3 find_room_id.py
# Change rooms in-game and watch for address changes
```

## 📚 Documentation

- **`README_chaos.md`** - Detailed documentation for the chaos generation scripts
- **`README_grappling_beam.md`** - Technical details about memory manipulation

## 🎯 Quick Start Guide

1. **Start RetroArch** with Super Metroid loaded
2. **Enable network control** in RetroArch settings (port 55355)
3. **Run the recommended script:**
   ```bash
   cd chaos_scripts/
   python3 super_metroid_persistent_chaos.py --insane
   ```
4. **Change rooms** to see persistent chaos effects!
5. **Press Ctrl+C** to stop and restore original graphics

## ⚠️ Safety Notes

- All scripts **backup original values** and restore on exit
- **Ctrl+C** always restores the game to normal state
- Effects are **visual only** - no save game corruption
- Scripts pause when game is not detected

## 🎨 Script Comparison

| Script | Persistence | Palette | Sprites | Map Focus | Intensity |
|--------|-------------|---------|---------|-----------|-----------|
| `persistent_chaos.py` | ✅ Yes | ✅ Yes | ✅ Yes | 🟡 Some | 🔥🔥🔥🔥🔥 |
| `map_color_chaos.py` | ❌ No | ✅ Yes | ❌ No | ✅ Only | 🔥🔥🔥 |
| `chaos.py` | ❌ No | ✅ Yes | ✅ Yes | ✅ Heavy | 🔥🔥🔥🔥 |

## 🎪 Fun Combinations

```bash
# Gentle persistent rainbow
python3 super_metroid_persistent_chaos.py --rainbow --speed 1

# Extreme sprite corruption
python3 super_metroid_persistent_chaos.py --sprites-only --psychedelic --turbo

# Map-focused visual decay
python3 super_metroid_map_color_chaos.py --psychedelic --speed 5

# Ultimate chaos experience
python3 super_metroid_persistent_chaos.py --insane
```

## 🔧 Troubleshooting

- **"Game not detected"** - Make sure RetroArch network control is enabled
- **"Connection failed"** - Check RetroArch is running on port 55355
- **Effects don't persist** - Use `persistent_chaos.py` instead of other scripts
- **Too intense** - Lower `--speed` or use `--palette-only`

---

**Have fun creating visual chaos in Zebes! 🌍✨**