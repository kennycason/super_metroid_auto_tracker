# Super Metroid HUD Kotlin Server - Reserve Energy Fix

## Issue Description

The Super Metroid HUD Kotlin server was displaying reserve energy incorrectly in the API response. The reserve energy was showing as 300/0 instead of 300/300, which meant that the max_reserve_energy value was not being parsed correctly from the game state.

## Root Cause

After investigating the code, I identified two issues:

1. **Incorrect Offset in parseBasicStats Method**: The offset for max_reserve_energy in the parseBasicStats method was incorrect. It was set to 16, but it should have been 18 to match the memory address in the memory map.

2. **Default Values Not Included in JSON Output**: The encodeDefaults setting in the JSON serialization configuration was not enabled, which meant that properties with default values (such as maxReserveEnergy with a value of 0) were not included in the JSON output.

## Changes Made

### 1. Fixed Offset in parseBasicStats Method

In the GameStateParser.kt file, I updated the offset for max_reserve_energy in the parseBasicStats method to match the memory address in the memory map:

```kotlin
// Before
"max_reserve_energy" to data.readInt16LE(16),
"reserve_energy" to data.readInt16LE(18)

// After
"max_reserve_energy" to data.readInt16LE(18),
"reserve_energy" to data.readInt16LE(20)
```

### 2. Enabled encodeDefaults in JSON Serialization

In the HttpServer.kt file, I enabled the encodeDefaults setting in the JSON serialization configuration to ensure that properties with default values are included in the JSON output:

```kotlin
// Before
json(Json {
    prettyPrint = true
    isLenient = true
})

// After
json(Json {
    prettyPrint = true
    isLenient = true
    encodeDefaults = true
})
```

## Verification

After making these changes, I restarted the server and verified that the reserve energy is now displayed correctly in the API response:

```json
"reserve_energy": 300,
"max_reserve_energy": 300,
```

## Additional Notes

The issue with the offset in the parseBasicStats method was due to a mismatch between the memory addresses in the memory map and the offsets used in the parseBasicStats method. The memory addresses in the memory map are:

```kotlin
"reserve_energy" to 0x7E09D6,
"max_reserve_energy" to 0x7E09D4,
```

When reading from the basic stats data, which starts at 0x7E09C2, the offsets should be:

- max_reserve_energy: 0x7E09D4 - 0x7E09C2 = 18
- reserve_energy: 0x7E09D6 - 0x7E09C2 = 20

By fixing these offsets, the server now correctly reads the max_reserve_energy and reserve_energy values from the basic stats data.

## Conclusion

The reserve energy display issue has been fixed by updating the offset for max_reserve_energy in the parseBasicStats method and enabling the encodeDefaults setting in the JSON serialization configuration. The server now correctly displays reserve energy as 300/300 in the API response.
