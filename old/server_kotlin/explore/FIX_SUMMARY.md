# Super Metroid HUD Kotlin Server - Fix Summary

## Issues Fixed

1. **Port Configuration**: Server now consistently uses port 8082
2. **Reserve Energy Display**: Fixed incorrect display of reserve energy (300/0 → 300/300)

## Changes Made

### Port Configuration Fix
- Modified `manage_server.sh` to always use port 8082

### Reserve Energy Display Fix
- Fixed memory offset in `GameStateParser.kt`:
  ```
  // Before
  "max_reserve_energy" to data.readInt16LE(16)
  // After
  "max_reserve_energy" to data.readInt16LE(18)
  ```
- Enabled `encodeDefaults` in JSON serialization in `HttpServer.kt`

## Verification
- Server consistently starts on port 8082
- API response now correctly shows reserve energy as 300/300

## Documentation
- Created detailed documentation in `explore/RESERVE_ENERGY_FIX.md`
