# AutoSplits Engine

## Overview

`AutoSplitsEngine` (~1600 lines) is the core timing and split detection engine. It processes game state frames at 200ms intervals and triggers splits when speedrun conditions are met.

## Split Profiles

4 built-in profiles in `SplitProfiles.kt`:

| Profile | ID | Splits | Description |
|---------|-----|--------|-------------|
| KPDR Any% | `kpdr-any` | 24 | Standard route, early Ice Beam |
| KPDR Any% Late Ice | `kpdr-late-ice` | 23 | Ice after Plasma, no Spazer |
| Low% Ice | `low-percent-ice` | 17 | Minimal items (14%) |
| 100% | `hundred-percent` | 12 | Full collection |

## Split Condition Detection

Each split has a specific detection function. Logic matches the community ASL autosplitter exactly.

### Boss Splits
- **Boss flag transitions**: Detect when boss defeat flag changes from 0→1
- Requires `previousGameState` to have flag=0 and `currentGameState` to have flag=1
- Uses area-specific boss flag memory locations (bossFlags1-5)

### Special Splits
- **Ceres Station**: Ceres Ridley defeated AND exited Ceres area (areaId != 6)
- **Golden Four (G4)**: All 4 bosses defeated AND in Statues room (0xA66A)
- **Mother Brain 1**: In MB room + HP drops below 18000 threshold
- **Mother Brain 2**: In MB room + HP drops below 36000 threshold
- **Ship**: Zebes Ablaze (eventFlags bit) + Ship AI = 0xAA4F + MB defeated (tourianBosses bit)
- **Ceres Escape**: Room transition FROM Ceres Elevator room (0xDF45)

### Item/Beam Splits
- Detect when item/beam bit flag transitions from 0→1
- Each item has a specific bit in the equipment word

## Auto-Skip Logic

When resuming a run or loading a save state, the engine auto-skips splits whose conditions are already met:
1. Checks `isConditionAlreadyMet()` for current split
2. If met, records a synthetic split time and advances
3. Continues until finding an unmet condition

## Timer Management

- **Auto-start**: Detects game start transition (gameState changes from title/loading to gameplay)
- **Pause/Resume**: Spacebar toggles with 300ms debounce
- **Reset**: R key, saves partial run as incomplete
- **Set Timer**: Manual timer value entry (creates paused run)

## Personal Best Tracking

- **Pre-run freeze**: PBs frozen at run start so deltas don't change mid-run
- **Only complete runs** contribute to PBs (incomplete runs filtered out)
- **Best segments**: Tracked per-segment across all complete runs
- **Delta display**: Current segment vs PB segment (green = ahead, red = behind)

## Run Lifecycle

```
startNewRun() → processGameState() [loop] → triggerSplit() [repeated]
    ↓                                              ↓
pauseRun() / resumeRun()               Last split → completeRun()
    ↓                                              ↓
resetRun()                              onRunSaved callback
    ↓                                              ↓
storeIncompleteRun()                    FileStorageService + LiveSplitWriter
```

## Key State

- `currentSplitIndex`: Next split to trigger (0-based)
- `previousGameState`: Previous frame for transition detection
- `preRunPersonalBests`: Frozen PBs from run start
- `_splitsState`: StateFlow<SplitsState> for UI reactivity
