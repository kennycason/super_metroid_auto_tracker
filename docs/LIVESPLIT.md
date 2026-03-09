# LiveSplit Integration

## Overview

The tracker supports bidirectional integration with LiveSplit's `.lss` (LiveSplit Splits) XML format. This allows importing existing split files and exporting runs back to LiveSplit-compatible format.

## File Format

LSS files are XML with this structure:
```xml
<Run version="1.7.0">
  <GameName>Super Metroid</GameName>
  <CategoryName>Any% KPDR</CategoryName>
  <AttemptCount>787</AttemptCount>
  <AttemptHistory>
    <Attempt id="1" started="..." ended="...">
      <RealTime>01:23:45.6789000</RealTime>
    </Attempt>
  </AttemptHistory>
  <Segments>
    <Segment>
      <Name>Kraid</Name>
      <Icon>base64...</Icon>
      <BestSegmentTime><RealTime>...</RealTime></BestSegmentTime>
      <SplitTimes>
        <SplitTime name="Personal Best"><RealTime>...</RealTime></SplitTime>
      </SplitTimes>
      <SegmentHistory>
        <Time id="1"><RealTime>...</RealTime></Time>
      </SegmentHistory>
    </Segment>
  </Segments>
</Run>
```

## Time Format

LiveSplit uses **100-nanosecond ticks** (7-digit fractional part):
- Format: `HH:MM:SS.TTTTTTT`
- Example: `01:23:45.6789000` = 1h 23m 45.6789s
- Conversion: `ticks / 10_000 = milliseconds`

## Components

### LiveSplitParser (`LiveSplitParser.kt`)
- Parses `.lss` XML → `LiveSplitDocument`
- Handles both `HH:MM:SS.TTTTTTT` and `MM:SS.TTTTTTT` formats
- Validates root element is `<Run>`

### LiveSplitWriter (`LiveSplitWriter.kt`)
- Writes `LiveSplitDocument` → `.lss` XML
- Version 1.7.0 format
- Omits XML declaration (matches LiveSplit behavior)
- Preserves icons, attempt history, segment history

### LiveSplitConverter (`LiveSplitConverter.kt`)
- **Segment name → split ID mapping**: ~60 known segment names mapped to internal IDs
- **Split ID → type mapping**: boss/item/beam/event classification
- `toSplitProfile()`: LSS → internal SplitProfile
- `toPersonalBest()`: Extract PB from LSS "Personal Best" comparison
- `fromRunHistory()`: Multiple runs → LSS document
- `fromRunSession()`: Single run → LSS (preserves existing doc data)

### SplitFormatService (`SplitFormatService.kt`)
- Manages read/write format preferences (JSON vs LSS)
- `handleRunSaved()`: Callback that appends runs to LSS file after each run
- `loadLiveSplitFile()`: Loads LSS and derives SplitsState
- Profile resolution: maps LSS profiles to canonical built-in profiles

### ExportRunsToLiveSplit (`ExportRunsToLiveSplit.kt`)
- CLI tool: `./gradlew exportToLiveSplit`
- Exports all JSON runs → LSS files at `~/.smtracker/exports/`
- Verifies output by parsing back

## Segment Name Mapping

The converter maps common LiveSplit segment names (case-insensitive) to internal split IDs:

| LSS Name | Internal ID |
|----------|------------|
| "kraid" | kraid |
| "morph ball", "morph" | morph_ball |
| "first missile" | first_missile |
| "mother brain 1", "mb1" | mother_brain_1 |
| "pb" | first_power_bomb |
| "g4" | golden_four |
| ... | (~60 total mappings) |

Unknown names are sanitized: spaces/special chars → underscores, lowercased.

## Run Appending Flow

When a run completes:
1. `AutoSplitsEngine.triggerSplit()` calls `onRunSaved` callback
2. `SplitFormatService.handleRunSaved()` receives the run
3. If LSS writing is enabled and profile matches:
   - Loads existing LSS file
   - Calls `LiveSplitConverter.fromRunSession()` with existing doc
   - Increments attempt count, appends to attempt history
   - Updates best segment times if beaten
   - Writes back via `LiveSplitWriter`

## Test Coverage

- `LiveSplitParserTest`: Time parsing, round-trips, 100%/KPDR file parsing, edge cases
- `LiveSplitWriterTest`: Round-trip preservation, file I/O
- `LiveSplitConverterTest`: Name mapping, profile conversion, PB extraction
- `LiveSplitWriteIntegrationTest`: Full write flow, multi-run appending, DNF handling
