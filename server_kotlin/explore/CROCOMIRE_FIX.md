# Super Metroid HUD Kotlin Server - Crocomire Detection Fix

## Issue Description

The Kotlin server was not correctly detecting Crocomire as beaten in Super Metroid. While other bosses were being detected correctly, Crocomire was showing as `false` in the `/api/status` endpoint even though it had been defeated in the game.

## Root Cause

After investigating the code and comparing it with the Python implementation, we identified the following issue:

In the BackgroundPoller.kt file, Crocomire data was being read from memory address 0x7ED82C:

```kotlin
println("🔄 ReadGameState: Reading crocomire (0x7ED82C)...")
val crocomire = udpClient.readMemoryRange(0x7ED82C, 2)
```

But in the Python implementation (background_poller_server.py), Crocomire data is read from memory address 0x7ED829:

```python
crocomire = self.udp_reader.read_memory_range(0x7ED829, 2)
```

This discrepancy in memory addresses was causing the Crocomire detection to fail. The Kotlin implementation was reading from the wrong memory address.

## Changes Made

We updated the memory address for Crocomire in the BackgroundPoller.kt file to match the Python implementation:

```kotlin
println("🔄 ReadGameState: Reading crocomire (0x7ED829)...")
val crocomire = udpClient.readMemoryRange(0x7ED829, 2)
```

## Verification

The fix was verified through:

1. **Unit Tests**: The existing tests for Crocomire detection passed successfully, confirming that the detection logic itself was correct.

2. **Server Testing**: We restarted the server and tested the `/api/status` endpoint, which now correctly shows Crocomire as beaten:

```json
"bosses": {
    "bomb_torizo": true,
    "kraid": true,
    "spore_spawn": true,
    "mother_brain": false,
    "crocomire": true,  // Now correctly showing as true
    "phantoon": true,
    "botwoon": true,
    "draygon": true,
    "ridley": true,
    "golden_torizo": true,
    "mother_brain_1": false,
    "mother_brain_2": false,
    "samus_ship": false
}
```

## Additional Notes

It's worth noting that both `crocomire` and `bossPlus1` are now reading from the same memory address (0x7ED829). This is consistent with the Python implementation and doesn't cause any conflicts.

The detection logic for Crocomire remains unchanged:

```kotlin
val crocomireDefeated = ((crocomireValue and 0x0002) != 0) && (crocomireValue >= 0x0202)
```

This logic checks if:
1. Bit 1 (0x0002) is set in the crocomireValue
2. The crocomireValue is greater than or equal to 0x0202

## Conclusion

This fix ensures that the Kotlin server correctly detects Crocomire as beaten in Super Metroid, providing accurate game state information to the HUD. The implementation now matches the Python implementation, which was known to work correctly.
