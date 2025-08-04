# Mother Brain Phase Detection Bootstrap Implementation

## Overview

This document outlines the implementation approach for the Mother Brain Phase Detection Bootstrap functionality in the Super Metroid HUD Kotlin server.

## Implementation Steps

### 1. Add setMotherBrainComplete method to GameStateParser.kt

```kotlin
/**
 * Sets Mother Brain phases based on current game state
 * Used for bootstrapping the MB cache when loading a save file
 */
suspend fun setMotherBrainComplete(
    mainBossesData: ByteArray?,
    escapeTimerData: ByteArray?,
    gameState: GameState
) = mutex.withLock {
    // Check if current state indicates MB progress
    val hasHyperBeam = gameState.beams["hyper"] == true
    val inTourian = gameState.areaId == 5
    val highHealth = gameState.maxHealth > 500
    val highMissiles = gameState.maxMissiles > 100
    
    // Check main boss bit
    val motherBrainDefeated = mainBossesData?.let { 
        (it[0].toInt() and 0x01) != 0 
    } ?: false
    
    // Check escape timer
    val escapeTimerActive = escapeTimerData?.let {
        it.size >= 2 && it.readInt16LE(0) > 0
    } ?: false
    
    // Set MB1 if main boss bit is set or we have clear post-game indicators
    if (motherBrainDefeated || hasHyperBeam || (inTourian && highHealth)) {
        motherBrainPhaseState["mb1_detected"] = true
        println("🧠 MB1 BOOTSTRAP: Setting MB1 complete based on game state")
    }
    
    // Set MB2 if escape timer is active or we have hyper beam
    if (escapeTimerActive || hasHyperBeam) {
        motherBrainPhaseState["mb2_detected"] = true
        println("🧠 MB2 BOOTSTRAP: Setting MB2 complete based on game state")
    }
    
    println("🧠 MB Cache after bootstrap: MB1=${motherBrainPhaseState["mb1_detected"]}, MB2=${motherBrainPhaseState["mb2_detected"]}")
}
```

### 2. Update bootstrapMbCacheIfNeeded in BackgroundPoller.kt

```kotlin
private suspend fun bootstrapMbCacheIfNeeded(gameState: GameState) {
    try {
        val health = gameState.health
        val maxHealth = gameState.maxHealth
        val missiles = gameState.missiles
        
        // Don't bootstrap if it looks like a new file
        if (health <= 99 && missiles <= 5) {
            println("🚫 SKIPPING BOOTSTRAP: Detected new save file (Health: $health/$maxHealth, Missiles: $missiles)")
            return
        }
        
        // Re-read key memory addresses for bootstrap
        val mainBossesData = udpClient.readMemoryRange(0x7ED828, 2)
        val escapeTimer1Data = udpClient.readMemoryRange(0x7E0943, 2)
        
        // Call parser's setMotherBrainComplete method
        parser.setMotherBrainComplete(mainBossesData, escapeTimer1Data, gameState)
        println("✅ MB cache bootstrap completed")
        
    } catch (e: Exception) {
        println("Error during MB bootstrap: ${e.message}")
    }
}
```

### 3. Add API endpoint for manual MB completion

In `HttpServer.kt`, add a route for `/api/manual-mb-complete`:

```kotlin
get("/api/manual-mb-complete") {
    try {
        // Force set MB completion in the parser
        poller.parser.motherBrainPhaseState["mb1_detected"] = true
        poller.parser.motherBrainPhaseState["mb2_detected"] = true
        
        val response = mapOf(
            "message" to "MB1 and MB2 manually set to completed",
            "mb1" to true,
            "mb2" to true
        )
        call.respond(response)
        println("🔧 Manual MB completion triggered via API")
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to e.message)
        )
    }
}
```

## Testing

1. Add a test case in `MotherBrainPhaseDetectionTest.kt` to verify the `setMotherBrainComplete` method works correctly.
2. Test the bootstrap functionality by loading a save file with indicators of post-game state.
3. Test the manual MB completion API endpoint.

## Expected Results

With these changes, the server should correctly detect Mother Brain phases when loading a save file, which is crucial for accurate end-game detection. This will ensure that the `samus_ship` flag is correctly set to `true` for completed games.
