# Super Metroid Map Randomizer - Memory Research

Research into Map Randomizer memory locations and data structures for integration into the Super Metroid Auto Tracker.

**Source Repository:** `/Users/kenny/code/MapRandomizer/`

## Memory Addresses

### Seed Identification

| Address | Size | Type | Description |
|---------|------|------|-------------|
| `$dfff00` | 4 bytes | uint32 (LE) | **Display Seed Hash** - Used for enemy name display on title screen |
| `$dffef0` | 16 bytes | ASCII string | **Seed Name** - Null-terminated URL-safe string (e.g., "mCGLkYzMD") |
| `$dfff05` | 2 bytes | uint16 (LE) | **Settings Flags** - Bit 0: walljump-boots collectible exists |
| `$dfff07` | 2 bytes | uint16 (LE) | **Unpause lag frames** - QoL setting (19 or 40 frames) |

### Stats & Timers (SRAM - Global)

| Address | Size | Description |
|---------|------|-------------|
| `$701E10` | 4 bytes | Total timer (frames) |
| `$701E14` | 2 bytes | Save count |
| `$701E16` | 2 bytes | Death count |
| `$701E18` | 2 bytes | Reload count |
| `$701E1A` | 2 bytes | Loadback count |
| `$701E1C` | 2 bytes | Reset count |
| `$701E1E` | 4 bytes | Final time |
| `$701E22` | 4 bytes | Pause time |
| `$701E26` | 4 bytes | Area 0 time (Crateria) |
| `$701E2A` | 4 bytes | Area 1 time (Brinstar) |
| `$701E2E` | 4 bytes | Area 2 time (Norfair) |
| `$701E32` | 4 bytes | Area 3 time (Wrecked Ship) |
| `$701E36` | 4 bytes | Area 4 time (Maridia) |
| `$701E3A` | 4 bytes | Area 5 time (Tourian) |
| `$701E3E` | 4 bytes | Area 6 time (Ceres/Debug) |

### Stats & Timers (RAM - Local)

| Address | Size | Description |
|---------|------|-------------|
| `$7efe06` | varies | Item collection times |

### Randomizer State

| Address | Size | Description |
|---------|------|-------------|
| `$09EC` | 2 bytes | Number of disabled E-Tanks |
| `$1F70` | 2 bytes | Spin lock enabled flag |
| `$1F72` | 2 bytes | Last Samus map X position |
| `$1F74` | 2 bytes | Last Samus map Y position |
| `$1F7A` | 2 bytes | Loadback ready flag |
| `$1F7C` | 2 bytes | NMI timer-only flag |
| `$1F7E` | 2 bytes | Previous room pointer |
| `$1F80` | 1 byte | NMI frame counter |
| `$1F81` | 1 byte | NMI pause counter |
| `$1F82` | 1 byte | NMI area 0 counter |
| `$1F83` | 1 byte | NMI area 1 counter |
| `$1F84` | 1 byte | NMI area 2 counter |
| `$1F85` | 1 byte | NMI area 3 counter |
| `$1F86` | 1 byte | NMI area 4 counter |
| `$1F87` | 1 byte | NMI area 5 counter |
| `$1F88` | 1 byte | NMI area 6 counter |
| `$7EF500` | varies | Room map tile graphics data |

### Objectives

| Address | Size | Description |
|---------|------|-------------|
| `$82FFFC` | 2 bytes | Number of objectives (bits 0-15) |
| `$8FEBC0` | varies | Objective addresses (max 20) |
| `$8FEBC0 + (2*20)` | varies | Objective bitmasks |

### ROM Constants

| Address | Size | Description |
|---------|------|-------------|
| `$7FC0` | 21 bytes | Cartridge name: "SUPERMETROID MAPRANDO" |
| `$85A100` | varies | Special door reveal table |

## Data Structures

### Seed Information (from Rust source)

```rust
pub struct Randomization {
    pub objectives: Vec<Objective>,
    pub save_animals: SaveAnimals,
    pub map: Map,
    pub locked_doors: Vec<LockedDoor>,
    pub item_placement: Vec<Item>,
    pub start_location: StartLocation,
    pub escape_time_seconds: f32,
    pub seed: usize,              // Random seed number
    pub display_seed: usize,      // Seed hash for display
    pub seed_name: String,        // URL-safe seed name
}
```

### Settings Structure

```rust
pub struct RandomizerSettings {
    pub version: usize,
    pub name: Option<String>,
    pub skill_assumption_settings: SkillAssumptionSettings,
    pub item_progression_settings: ItemProgressionSettings,
    pub quality_of_life_settings: QualityOfLifeSettings,
    pub objective_settings: ObjectiveSettings,
    pub map_layout: String,
    pub doors_mode: DoorsMode,
    pub start_location_settings: StartLocationSettings,
    pub save_animals: SaveAnimals,
    pub other_settings: OtherSettings,
}
```

## Seed Display Format

The seed hash (`$dfff00`) is converted to enemy names using a lookup table:

| Value | Enemy Name | Value | Enemy Name | Value | Enemy Name | Value | Enemy Name |
|-------|-----------|-------|-----------|-------|-----------|-------|-----------|
| 0 | GEEMER | 8 | BEETOM | 16 | BOYON | 24 | PUYO |
| 1 | RIPPER | 9 | OWTCH | 17 | CHOOT | 25 | YARD |
| 2 | ATOMIC | 10 | ZEBBO | 18 | KAGO | 26 | ZOA |
| 3 | POWAMP | 11 | ZEELA | 19 | SKREE | 27 | FUNE |
| 4 | SCISER | 12 | HOLTZ | 20 | COVERN | 28 | GAMET |
| 5 | NAMIHE | 13 | VIOLA | 21 | EVIR | 29 | GERUTA |
| 6 | PUROMI | 14 | WAVER | 22 | TATORI | 30 | SOVA |
| 7 | ALCOON | 15 | RINKA | 23 | OUM | 31 | BULL |

The 4-byte seed hash is split into 4 values (each 0-31), and each is converted to an enemy name.

## Settings Available (Not in Memory - Stored in ROM/Seed Data)

These settings are baked into the ROM when the seed is generated and are **not** accessible via memory reading:

### Skill Assumption Settings
- Preset name (e.g., "Basic", "Intermediate", "Advanced")
- Shinespark tiles
- Heated shinespark tiles
- Speed ball tiles
- Shinecharge leniency frames
- Resource multiplier
- Gate glitch leniency
- Tech settings (list of enabled/disabled techniques)
- Boss proficiency levels (Phantoon, Draygon, Ridley, Botwoon, Mother Brain)
- Escape timer multiplier

### Item Progression Settings
- Preset name (e.g., "Normal", "Fast")
- Progression rate
- Item placement style
- Item priority strength
- Random tank enabled
- Spazer before plasma
- Item pool preset
- Starting items preset

### Quality of Life Settings
- Preset name (e.g., "Default", "Maximum")
- Initial map reveal settings
- Item markers
- Room outline revealed
- Mother Brain fight settings
- Fast elevators
- Fast doors
- Fast pause menu
- Fanfares
- Respin
- Infinite space jump
- All items spawn
- E-tank refill mode
- Buffed drops

### Objective Settings
- Objective mode (e.g., "Bosses", "Minibosses", "Metroids")
- Number of objectives required

### Map Layout
- "Standard", "Wild", "Small"

### Doors Mode
- "Ammo", "Blue", "Beam"

### Start Location
- "Ship", "Random", "Escape"

### Save Animals
- "Yes", "No", "Random"

## Metadata from Seed Files (Not in ROM)

When a seed is generated, additional metadata is stored in the seed files on the web server but **not** written to the ROM:

- **Created timestamp**: When the seed was generated
- **Version**: Map Rando version number (e.g., 117)
- **Random seed number**: The actual numeric seed (e.g., 1872813061)
- **Settings preset names**: Custom or preset names for each category

## Implementation Notes

### What Can Be Read from Memory

✅ **Available:**
- Seed name (short identifier like "mCGLkYzMD")
- Seed hash (for display/verification)
- Walljump-boots collectible flag
- Run statistics (time, saves, deaths, etc.)
- Current randomizer state (disabled E-tanks, map position, etc.)
- Objectives count and addresses

❌ **Not Available:**
- Settings presets (Skill assumption, Item progression, QoL, etc.)
- Map layout type
- Doors mode
- Start location mode
- Created timestamp
- Random seed number
- Version number

### Future Integration

To display full seed information in the tracker's Map Rando Icon View:

1. **From Memory:** Read seed name from `$dffef0` (16 bytes)
2. **From API:** Use seed name to fetch full metadata from maprando.com API:
   ```
   GET https://maprando.com/api/seed/{seed_name}
   ```
3. **Display:** Show all metadata fetched from API alongside memory-read data

### Example Memory Read Implementation

```kotlin
fun readMapRandoSeedName(): String? {
    val seedNameBytes = ByteArray(16)
    for (i in 0..15) {
        seedNameBytes[i] = readMemory(0xdffef0 + i)
    }
    // Find null terminator
    val nullIndex = seedNameBytes.indexOf(0.toByte())
    return if (nullIndex > 0) {
        String(seedNameBytes, 0, nullIndex, Charsets.US_ASCII)
    } else {
        null
    }
}

fun readMapRandoDisplaySeed(): UInt? {
    val byte0 = readMemory(0xdfff00).toUInt()
    val byte1 = readMemory(0xdfff01).toUInt()
    val byte2 = readMemory(0xdfff02).toUInt()
    val byte3 = readMemory(0xdfff03).toUInt()
    return byte0 or (byte1 shl 8) or (byte2 shl 16) or (byte3 shl 24)
}
```

## References

- **Constants:** `MapRandomizer/patches/src/constants.asm`
- **Seed Display:** `MapRandomizer/patches/src/seed_hash_display.asm`
- **Stats:** `MapRandomizer/patches/src/stats.asm`
- **Patch Logic:** `MapRandomizer/rust/maprando/src/patch.rs`
- **Settings:** `MapRandomizer/rust/maprando/src/settings.rs`
- **Randomization:** `MapRandomizer/rust/maprando/src/randomize.rs`

## Notes

- All memory addresses are in SNES format (bank:address)
- Use `snes2pc()` conversion for ROM addresses
- Stats are stored in SRAM at `$70:xxxx` (battery-backed)
- Randomizer state is in WRAM at `$00:xxxx` and `$7E:xxxx`
- Most settings are NOT accessible via memory - they must be fetched from the seed metadata via API

