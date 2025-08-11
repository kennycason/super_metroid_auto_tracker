# Super Metroid Grappling Beam Script

This script uses RetroArch's UDP network interface to give Samus the Grappling Beam in Super Metroid.

## How it works

The script:
1. Connects to RetroArch via UDP (port 55355)
2. Reads the items memory at address `0x7E09A4` (2 bytes)
3. Sets the grappling beam bit (`0x4000`) in the items value
4. Writes the new value back to memory
5. Verifies the change was successful

## Super Metroid Memory Layout

Based on the tracker server code, here are the key memory addresses:

```python
memory_map = {
    'health': 0x7E09C2,
    'max_health': 0x7E09C4,
    'missiles': 0x7E09C6,
    'max_missiles': 0x7E09C8,
    'supers': 0x7E09CA,
    'max_supers': 0x7E09CC,
    'power_bombs': 0x7E09CE,
    'max_power_bombs': 0x7E09D0,
    'reserve_energy': 0x7E09D6,
    'max_reserve_energy': 0x7E09D4,
    'room_id': 0x7E079B,
    'area_id': 0x7E079F,
    'items': 0x7E09A4,    # <-- Items bitmask (2 bytes)
    'beams': 0x7E09A8,    # <-- Beam weapons bitmask (2 bytes)
    'bosses': 0x7ED828,   # <-- Boss defeat status
}
```

## Items Bitmask (at 0x7E09A4)

The items are stored as a 2-byte little-endian value with these bit flags:

```python
items_bits = {
    'varia': 0x0001,      # Varia Suit
    'spring': 0x0002,     # Spring Ball  
    'morph': 0x0004,      # Morph Ball
    'screw': 0x0008,      # Screw Attack
    'gravity': 0x0020,    # Gravity Suit
    'hijump': 0x0100,     # Hi-Jump Boots
    'space': 0x0200,      # Space Jump
    'bombs': 0x1000,      # Bombs
    'speed': 0x2000,      # Speed Booster
    'grapple': 0x4000,    # Grappling Beam  <-- This one!
    'xray': 0x8000,       # X-Ray Scope
}
```

## Prerequisites

1. **RetroArch must be running** with Super Metroid loaded
2. **Network commands must be enabled** in RetroArch:
   - Go to Settings → Network → Network Commands → ON
   - Or set `network_cmd_enable = "true"` in retroarch.cfg
3. **RetroArch should be listening on port 55355** (default)

## Usage

```bash
# Give Samus the grappling beam
python3 give_grappling_beam.py
```

## RetroArch UDP Commands Used

The script uses these RetroArch network commands:

- `GET_STATUS` - Check if game is running
- `READ_CORE_MEMORY 0x7E09A4 2` - Read current items value
- `WRITE_CORE_MEMORY 0x7E09A4 XX XX` - Write new items value

## Example Output

```
🚀 Starting RetroArch Grappling Beam Giver...
🎮 Super Metroid Grappling Beam Giver
========================================
🔍 Checking if Super Metroid is running...
✅ Super Metroid detected!
📖 Reading items memory at 0x7E09A4...
📊 Current items value: 0x2104 (8452)
🔧 Grappling beam status: ❌ NOT EQUIPPED
⚡ Adding grappling beam...
   Old value: 0x2104
   New value: 0x6104
🔍 Verifying grappling beam was added...
🎉 SUCCESS! Samus now has the grappling beam!
   Verified value: 0x6104
```

## Other Possible Modifications

You can modify this script to give other items by changing the bit mask:

```python
# Give multiple items at once
MORPH_BALL = 0x0004
BOMBS = 0x1000
GRAPPLE = 0x4000
SPACE_JUMP = 0x0200

# Give morph ball + bombs + grapple + space jump
new_items = current_items | MORPH_BALL | BOMBS | GRAPPLE | SPACE_JUMP
```

Or modify other aspects like health, missiles, etc. by writing to their respective addresses.

## Troubleshooting

- **"Super Metroid is not running"**: Load Super Metroid in RetroArch first
- **"UDP timeout"**: Enable network commands in RetroArch settings
- **"Failed to read memory"**: Make sure you're using a compatible SNES core (bsnes, Snes9x, etc.)
- **"Verification failed"**: The memory write succeeded but the game might have overwritten it immediately