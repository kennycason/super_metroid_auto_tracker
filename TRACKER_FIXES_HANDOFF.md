# Tracker Fixes Handoff

## Context
These issues were discovered while working in the twitch bot repo but belong here in the auto_tracker codebase.

---

## Issue 1: Deleted Run Still Showing in PB/Best Possible

### What happened
- User deleted the 00:28.14 run (containment-chamber-puzzles profile, run file `containment-chamber-puzzles_2026-04-14_22-56-25_1776232585695.json`)
- The run was removed from the runs directory BUT the PB/Best Possible/Personal Best values were NOT recalculated
- The LSS file still has `<SplitTime name="Personal Best">` entries from that deleted run (last segment shows `00:00:28.1440000`)

### What was already fixed (manually via python script)
- `run-summaries.json` was recalculated from remaining 23 run files (14 completed)
- New best: 1211203ms (20:11.203) from `run_1776120578572`
- Sum of best segments (best possible): 818428ms (13:38.428) across 25 splits
- The stale JSON run file was moved to `~/.smtracker/backups/`

### What still needs fixing in code

#### A. `deleteLssAttempt()` in `SplitFormatService.kt` (line ~351)
Currently removes the attempt and segment history entries but does NOT recalculate:
1. `<SplitTime name="Personal Best">` tags on each segment
2. `<BestSegmentTime>` tags on each segment

After deleting an attempt, it should:
1. Find the next-best completed attempt from remaining `attemptHistory`
2. Recalculate PB split times from that attempt's segment history entries
3. Recalculate best segment times across ALL remaining segment history entries
4. Update the `segments` in the LSS document with new PB and best segment values

#### B. Delete flow should recalculate `run-summaries.json`
When a run is deleted (either via LSS attempt delete or JSON file delete), `run-summaries.json` should be recalculated. The recalculation needs to:
1. Find all remaining completed runs for the profile
2. Find the new best total time
3. Recalculate best split times (sum of best segments) across all remaining runs
4. Update the profile entry in `run-summaries.json`

#### C. Delete should always backup (already working)
`deleteLssAttempt()` already calls `fileStorageService.backupFileSync(file)` before writing. Good.
`FileStorageService.deleteRun()` already calls `backupFile(runFile)` before `runFile.delete()`. Good.
Backups go to `~/.smtracker/backups/`.

---

## Issue 2: Palette Persistence Across Room Transitions (Twitch Bot)

This was addressed in the twitch bot code. When a color effect is applied via `!sm color <effect>`, it's now stored and re-applied on room transitions. The 1-3 second delay is inherent to CGRAM reloading - the game loads palettes from ROM on room entry, and we overwrite them after detecting the room change.

### Possible improvement for auto_tracker
If the auto_tracker ever needs to expose a palette API, the approach is:
- CGRAM lives at WRAM `$7EC000-$7EC1FF` (256 colors, 2 bytes each in BGR555)
- Game reloads CGRAM from ROM data on every room transition
- To persist: detect room change via `roomId`, wait ~100ms for CGRAM load, then re-apply
- Faster approach: hook the DMA transfer or write to ROM palette data directly (but ROM writes may not work on all cores)

---

## Issue 3: Physics Addresses (Twitch Bot)

Physics modifications now use ROM momentum addresses confirmed working in super_metroid_chaos:
- `0x81F71` - Air speed (non-spin jump)
- `0x81F7D` - Air speed (spin jump)
- `0x81F65` - Run max speed
- `0x81FA1` - Air speed (ledge fall)
- `0x82049` - Wall jump speed

These are single-byte values (vanilla 1-2, safe range 0-4). They persist until overwritten and are re-read by the game engine each frame. Also tracked with `activePreset` for re-application on room transitions.

WRAM addresses for one-shot effects:
- `$7E0B2E` - Samus Y speed (signed 16-bit)
- `$7E0B2C` - Samus X speed (signed 16-bit)

---

## File Locations Reference

| File | Path | Purpose |
|---|---|---|
| Run summaries | `~/.smtracker/run-summaries.json` | Cached best times, best splits per profile |
| Runs directory | `~/.smtracker/runs/` | Individual run JSON files |
| Backups | `~/.smtracker/backups/` | Deleted/backed-up files |
| LSS file | `~/.smtracker/containment-chamber-puzzles.lss` | LiveSplit format with PB, attempts, segment history |
| Delete logic | `SplitFormatService.kt:351` | `deleteLssAttempt()` |
| Storage logic | `FileStorageService.kt` | `deleteRun()`, `backupFile()` |
| Summary calc | Needs new function | Recalculate `run-summaries.json` from remaining runs |
