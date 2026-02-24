# LiveSplit File Architecture: ASL vs LSS

## Overview

LiveSplit uses two distinct file types that work together but serve completely different purposes.

## File Types

### `.lss` Files (LiveSplit Splits) — **Pure Data**

These are XML files containing **only** timing and metadata:

| Content | Description |
|---------|-------------|
| Split Names | User-defined names (e.g., "Bomb", "Varia", "G4", "Done") |
| Personal Best Times | Cumulative times for each split |
| Best Segment Times | "Gold splits" - fastest individual segment times |
| Attempt History | Record of every run attempt with timestamps |
| Game/Category Info | Game name, category name, platform, region |

**Key Point:** No code, no memory addresses, no game detection logic.

#### Example LSS Structure (simplified)
```xml
<Run version="1.7.0">
  <GameName>Super Metroid</GameName>
  <CategoryName>Any% KPDR</CategoryName>
  <AttemptCount>787</AttemptCount>
  <Segments>
    <Segment>
      <Name>Bomb</Name>
      <BestSegmentTime>
        <RealTime>00:03:42.1200000</RealTime>
      </BestSegmentTime>
    </Segment>
    <!-- more segments... -->
  </Segments>
  <AttemptHistory>
    <Attempt id="1" started="2024-01-15T10:30:00" ended="2024-01-15T11:15:00">
      <RealTime>00:45:23.4500000</RealTime>
    </Attempt>
    <!-- more attempts... -->
  </AttemptHistory>
</Run>
```

---

### `.asl` Files (Auto Splitter Language) — **Code/Logic**

These are C#-like scripts containing the "intelligence" for automatic splitting:

| Content | Description |
|---------|-------------|
| Memory Addresses | Where to read game data (e.g., `0x079B` for room ID) |
| Room IDs | Dictionary of room identifiers |
| Boss/Item Flags | Bit flags for tracking game state |
| Split Conditions | Logic for when to trigger splits |
| Start/Reset Logic | When to start/reset the timer |
| Emulator Support | Memory offset detection for various emulators |

#### Key Code Sections in `supermetroid.asl`

**Memory Watchers (addresses to monitor):**
```csharp
vars.watchers = new MemoryWatcherList
{
    new MemoryWatcher<ushort>(memoryOffset + 0x079B) { Name = "roomID" },
    new MemoryWatcher<byte>(memoryOffset + 0x079F) { Name = "mapInUse" },
    new MemoryWatcher<byte>(memoryOffset + 0x0998) { Name = "gameState" },
    new MemoryWatcher<byte>(memoryOffset + 0x09A4) { Name = "unlockedEquips2" },
    // ... more addresses
};
```

**Room ID Enum:**
```csharp
vars.roomIDEnum = new Dictionary<string, int> {
    { "landingSite", 0x91F8 },
    { "bombTorizo", 0x9804 },
    { "kraid", 0xA59F },
    { "phantoon", 0xCD13 },
    // ... 140+ rooms
};
```

**Split Conditions:**
```csharp
var ridley = settings["ridley"] 
    && (vars.watchers["norfairBosses"].Old & vars.bossFlagEnum["ridley"]) == 0 
    && (vars.watchers["norfairBosses"].Current & vars.bossFlagEnum["ridley"]) > 0 
    && vars.watchers["roomID"].Current == vars.roomIDEnum["ridley"];
```

---

## How They Work Together

```
┌─────────────────────────────────────────────────────────────────┐
│                         LiveSplit                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   .asl file (shared)              .lss file (personal)          │
│   ┌──────────────────────┐       ┌──────────────────────┐       │
│   │ Memory reading       │       │ Split names          │       │
│   │ Room IDs             │       │ Personal best times  │       │
│   │ Boss/item flags      │ ───►  │ Gold splits          │       │
│   │ Split detection      │triggers│ Attempt history     │       │
│   │ Start/reset logic    │ splits │ Category metadata   │       │
│   │ Emulator support     │       │                      │       │
│   └──────────────────────┘       └──────────────────────┘       │
│          CODE                           DATA                     │
│     (game-specific)                (runner-specific)             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Workflow

1. **Runner loads `.lss` file** — Sets up split names, shows previous PBs
2. **LiveSplit loads `.asl` file** — Either auto-detected or manually selected
3. **During gameplay:**
   - `.asl` reads game memory continuously
   - When conditions match (e.g., boss defeated), `.asl` triggers a split
   - Split time is recorded in memory (and saved to `.lss` when run ends)
4. **After run:** Updated times saved to `.lss` file

---

## Relationship to This Tracker

Our Super Metroid tracker has equivalent components:

| LiveSplit Component | Our Tracker Equivalent |
|---------------------|------------------------|
| `.asl` memory reading | `SNIMemoryAdapter.kt`, `RetroArchUdpClient.kt` |
| `.asl` room IDs | `RoomDatabase.kt` |
| `.asl` split conditions | `AutoSplitsEngine.kt`, `GameStateParser.kt` |
| `.lss` split names | `SplitProfiles.kt` |
| `.lss` timing data | `RunSession`, `PersonalBest`, `runs/*.json` |

### Import/Export Possibilities

**Importing `.lss` files could provide:**
- Seed PB times from existing LiveSplit history
- Import gold splits as best segment starting points
- Create new `SplitProfile` from segment names

**Exporting to `.lss` could provide:**
- Share runs with LiveSplit users
- Interoperability with streaming setups
- Backup/migration path

### Mapping Challenge

LiveSplit split names are user-defined strings, while our tracker uses structured IDs:

| LiveSplit Name | Our Split ID | Notes |
|----------------|--------------|-------|
| "Bomb" | `bomb` | Direct match |
| "Varia" | `varia_suit` | Name variation |
| "G4" | `golden_four` | Abbreviation |
| "Done" | `ship` | Different terminology |
| "Grapple" | `grapple_beam` | Would need to add |
| "Plasma" | `plasma_beam` | Would need to add |

A mapping layer would be needed for reliable import/export.

---

## References

- [LiveSplit GitHub](https://github.com/LiveSplit/LiveSplit)
- [Auto Splitters Documentation](https://github.com/LiveSplit/LiveSplit.AutoSplitters)
- [Super Metroid ASL](https://github.com/UNHchabo/AutoSplitters) — Source for `supermetroid.asl`
- [Super Metroid Room List](https://wiki.supermetroid.run/List_of_rooms_by_SMILE_ID)
