# Super Metroid Memory Map

## Memory Reading

The tracker reads SNES memory via two protocols:
- **SNI (gRPC)**: Preferred. Uses LoROM addressing on SnesABus
- **RetroArch (UDP)**: Fallback. Uses `READ_CORE_MEMORY` command

## Key Addresses

All addresses from `RetroArchUdpClient.SuperMetroidAddresses`:

### Player Stats (WRAM Bank 0x7E)
| Address | Size | Description |
|---------|------|-------------|
| 0x7E09C2 | 2 | Current Health |
| 0x7E09C4 | 2 | Max Health |
| 0x7E09C6 | 2 | Current Missiles |
| 0x7E09C8 | 2 | Max Missiles |
| 0x7E09CA | 2 | Current Super Missiles |
| 0x7E09CC | 2 | Max Super Missiles |
| 0x7E09CE | 2 | Current Power Bombs |
| 0x7E09D0 | 2 | Max Power Bombs |
| 0x7E09D4 | 2 | Reserve Energy |
| 0x7E09D6 | 2 | Max Reserve Energy |

### Equipment & Beams (WRAM)
| Address | Size | Description |
|---------|------|-------------|
| 0x7E09A2 | 2 | Equipped Items (bit flags) |
| 0x7E09A4 | 2 | Collected Items (bit flags) |
| 0x7E09A6 | 2 | Equipped Beams (bit flags) |
| 0x7E09A8 | 2 | Collected Beams (bit flags) |

#### Item Bit Flags (0x7E09A2 equipped / 0x7E09A4 collected)
| Bit | Hex | Item |
|-----|-----|------|
| 0 | 0x0001 | Varia Suit |
| 1 | 0x0002 | Spring Ball |
| 2 | 0x0004 | Morph Ball |
| 3 | 0x0008 | Screw Attack |
| 5 | 0x0020 | Gravity Suit |
| 8 | 0x0100 | Hi-Jump Boots |
| 9 | 0x0200 | Space Jump |
| 12 | 0x1000 | Bombs |
| 13 | 0x2000 | Speed Booster |
| 14 | 0x4000 | Grapple Beam |
| 15 | 0x8000 | X-Ray Scope |

#### Beam Bit Flags (0x7E09A6 equipped / 0x7E09A8 collected)
| Bit | Hex | Beam |
|-----|-----|------|
| 0 | 0x0001 | Wave |
| 1 | 0x0002 | Ice |
| 2 | 0x0004 | Spazer |
| 3 | 0x0008 | Plasma |
| 12 | 0x1000 | Charge |

### Location (WRAM)
| Address | Size | Description |
|---------|------|-------------|
| 0x7E079B | 2 | Room ID |
| 0x7E079F | 2 | Area ID (0-6) |
| 0x7E0998 | 2 | Game State |
| 0x7E0AF6 | 2 | Player X Position |
| 0x7E0AFA | 2 | Player Y Position |

### Boss Flags (WRAM)
| Address | Size | Description |
|---------|------|-------------|
| 0x7ED828 | 2 | Boss Flags 1 (Crateria/Brinstar bosses) |
| 0x7ED82C | 2 | Boss Flags 2 (Crocomire) |
| 0x7ED82A | 2 | Boss Flags 3 (Norfair: Ridley, Golden Torizo) |
| 0x7ED830 | 2 | Boss Flags 4 (Wrecked Ship: Phantoon) |
| 0x7ED82E | 2 | Boss Flags 5 (Maridia: Botwoon, Draygon) |

### Event & End-Game (WRAM)
| Address | Size | Description |
|---------|------|-------------|
| 0x7ED820 | 2 | Event Flags (Zebes Ablaze = bit for ship detection) |
| 0x7ED826 | 2 | Tourian Bosses (MB defeated flag) |
| 0x7E0FB2 | 2 | Ship AI (0xAA4F when ship ready) |
| 0x7ED824 | 2 | Metroid Room Flags (4 rooms) |

### Mother Brain (WRAM)
| Address | Size | Description |
|---------|------|-------------|
| 0x7E0FCC | 2 | Mother Brain HP |

### Map Rando (SRAM Bank 0x70 / ROM Bank 0xDF)
| Address | Size | Description |
|---------|------|-------------|
| 0x701F64 | 2 | Death Count |
| 0x701F66 | 2 | Quick Reload Count |
| 0x701F6A | 2 | Reset Count |
| 0xDFFEF0 | 16 | Seed Name (null-terminated ASCII) |

## Game State Values

From `GameStateConstants`:
| Value | State |
|-------|-------|
| 0 | LOADING |
| 2 | TITLE_SCREEN |
| 8 | NORMAL_GAMEPLAY |
| 11 | DOOR_TRANSITION |
| 12 | ELEVATOR |
| 14 | PAUSING |
| 18 | PAUSED |
| 20 | UNPAUSING |
| 31 | GAME_START_TRANSITION |
| 32 | CERES_CUTSCENE |
| 34 | (Unknown, treated as valid) |

## Area IDs

| ID | Area |
|----|------|
| 0 | Crateria |
| 1 | Brinstar |
| 2 | Norfair |
| 3 | Wrecked Ship |
| 4 | Maridia |
| 5 | Tourian |
| 6 | Ceres |
