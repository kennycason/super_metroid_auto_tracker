# Super Metroid HUD Kotlin Server Caching Fix

## Issue Description

The server was experiencing a caching issue where it was not properly refreshing its game state data when a new ROM was loaded or when a reset was requested. The user tried to reset the cache using `/api/reset` but it didn't fix the issue. The API was still showing stale data from a previous ROM.

## Root Causes

After examining the code, we identified the following issues:

1. **Mock Client Usage**: The `BackgroundPoller` was using a `MockRetroArchUDPClient` in STATIC mode, which always returns the same static data regardless of the actual game state.

2. **Incomplete Cache Reset**: The `resetCache()` method in `BackgroundPoller` was not resetting all necessary state:
   - It reset the cache and lastUpdate, but not the bootstrapAttempted flag
   - It didn't reset the Mother Brain cache in the parser

3. **Missing API Endpoint**: The user was trying to use `/api/reset`, but the actual endpoint was `/api/reset-cache`.

## Changes Made

### 1. BackgroundPoller.kt

```kotlin
// Before:
private val udpClient = MockRetroArchUDPClient().apply {
    setMockMode(MockRetroArchUDPClient.MockMode.STATIC)
    setStaticState(1) // 0=Crateria, 1=Norfair, 2=Lower Norfair
}
// private val udpClient = RetroArchUDPClient()

// After:
private val udpClient = RetroArchUDPClient()

// For testing, uncomment this and comment out the line above
// private val udpClient = MockRetroArchUDPClient().apply {
//     setMockMode(MockRetroArchUDPClient.MockMode.STATIC)
//     setStaticState(1) // 0=Crateria, 1=Norfair, 2=Lower Norfair
// }
```

```kotlin
// Before:
suspend fun resetCache() = mutex.withLock {
    cache = ServerStatus()
    lastUpdate = 0L
    println("🔄 Background poller cache cleared")
}

// After:
suspend fun resetCache() = mutex.withLock {
    cache = ServerStatus()
    lastUpdate = 0L
    bootstrapAttempted = false
    
    // Also reset the Mother Brain cache in the parser
    try {
        parser.resetMotherBrainCache()
        println("🧠 Mother Brain cache reset")
    } catch (e: Exception) {
        println("⚠️ Failed to reset Mother Brain cache: ${e.message}")
    }
    
    println("🔄 Background poller cache fully reset")
}
```

### 2. HttpServer.kt

Added a new `/api/reset` endpoint that calls `poller.resetCache()`:

```kotlin
// Add /api/reset endpoint that redirects to /api/reset-cache
// This is for compatibility with the user's existing calls
get("/api/reset") {
    poller.resetCache()
    call.respondText("Cache reset successfully via /api/reset", ContentType.Text.Plain, HttpStatusCode.OK)
    println("🔄 Cache reset via /api/reset endpoint")
}
```

## Testing

We created test scripts in the `explore/` directory to verify the fixes:

1. **test_api.sh**: A bash script that tests the API endpoints using curl
2. **test_api.py**: A more comprehensive Python script that tests the API endpoints and displays the results in a readable format

See the `explore/README.md` file for detailed testing instructions.

## Expected Results

With these changes, the server should now:

1. Use the real RetroArchUDPClient to get real-time data from RetroArch
2. Properly reset all cache state when `/api/reset` or `/api/reset-cache` is called
3. Respond to the user's calls to `/api/reset`

This should fix the issue where the server was showing stale data from a previous ROM.
