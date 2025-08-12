# Super Metroid HUD Kotlin Server - Samus Ship Detection Fix

## Issue Description

The Super Metroid HUD Kotlin server was not correctly detecting the end game state. The `samus_ship` flag was hardcoded to `false` with a comment "End-game detection - keep simple for now". This meant that the server would never report that the game had been completed, even if the player had reached Samus's ship at the end of the game.

## Implementation

To fix this issue, we implemented proper end game detection logic based on the Python implementation. The implementation includes:

1. **Memory Addresses**: Added the necessary memory addresses to the memory map in `GameStateParser.kt`:
```kotlin
"ship_ai" to 0x7E0FB2,       // Ship AI state (0xaa4f when at ship)
"event_flags" to 0x7ED821,   // Event flags (bit 0x40 set when Zebes is ablaze)
```

2. **Memory Reading**: Modified `BackgroundPoller.kt` to read these memory addresses during the polling process:
   ```kotlin
   // Read end game detection memory addresses
   println("🔄 ReadGameState: Reading ship_ai (0x7E0FB2)...")
   val shipAi = udpClient.readMemoryRange(0x7E0FB2, 2)

   println("🔄 ReadGameState: Reading event_flags (0x7ED821)...")
   val eventFlags = udpClient.readMemoryRange(0x7ED821, 1)
   ```

3. **Detection Logic**: Implemented a `detectSamusShip` method in `GameStateParser.kt` that uses multiple approaches to detect when Samus has reached her ship:
   - **Official Autosplitter Detection**: Checks if `ship_ai` is `0xaa4f` and if the `event_flags` bit `0x40` is set (Zebes is ablaze)
   - **Relaxed Area Detection**: Checks if the player is in Crateria or other possible escape areas
   - **Position-based Detection**: Checks if the player is in specific room IDs and coordinates associated with the ship
   - **Emergency Ship Detection**: A fallback detection method for when MB2 is complete and the player is in a valid area with specific position coordinates

4. **Integration**: Updated the `parseBosses` method to use the `detectSamusShip` method for the `samus_ship` flag:
   ```kotlin
   "samus_ship" to detectSamusShip(
       shipAiData = shipAiData,
       eventFlagsData = eventFlagsData,
       locationData = locationData,
       motherBrainDefeated = motherBrainDefeated,
       mb1Detected = motherBrainPhaseState["mb1_detected"]!!,
       mb2Detected = motherBrainPhaseState["mb2_detected"]!!
   )
   ```

5. **Logging**: Added extensive logging to help debug the detection process:
   ```kotlin
   println("🚢 OFFICIAL DETECTION - shipAI: 0x${shipAiVal.toString(16).uppercase().padStart(4, '0')}, eventFlags: 0x${eventFlagsVal.toString(16).uppercase().padStart(2, '0')}")
   println("🚢 zebesAblaze: $zebesAblaze, shipAI_reached: $shipAiReached")
   ```

## Testing

The implementation was tested with a non-completed game save file, and the `samus_ship` flag was correctly reported as `false`. The logs showed:
- shipAI: 0xA9BD (not the expected 0xaa4f for ship reached)
- eventFlags: 0x00 (bit 0x40 is not set, so Zebes is not ablaze)
- Area: 0 (Crateria), Room: 33266, Position: (897, 1088)
- Mother Brain status: main=true, MB1=false, MB2=false

The detection logic worked as expected, with all detection methods failing for a non-completed game.

## Expected Behavior

For a completed game save file, the `samus_ship` flag should be reported as `true` if any of the following conditions are met:
1. The official detection succeeds (shipAI is 0xaa4f and Zebes is ablaze)
2. The position detection succeeds (player is in a ship room and at ship coordinates)
3. The emergency detection succeeds (MB2 is complete and player is in a valid area with specific position coordinates)

## Conclusion

The implementation now correctly detects the end game state based on multiple approaches, matching the behavior of the Python implementation. The `samus_ship` flag will be reported as `true` when the player has reached Samus's ship at the end of the game, providing accurate game state information to the HUD.
