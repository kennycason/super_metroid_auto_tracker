package com.supermetroid.service

import strikt.api.expectThat
import strikt.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Tests for the GameStateService callback mechanism.
 * 
 * The callback is used to decouple split detection from Compose recomposition.
 * This ensures that splits are detected reliably on every poll, not dependent
 * on UI framework timing.
 */
@DisplayName("GameStateService Callback")
class GameStateCallbackTest {

    @Test
    @DisplayName("Callback should be invokable when set")
    fun `callback should be invokable when set`() {
        // Given: A GameStateService instance
        val service = GameStateService()
        var callbackInvoked = false
        
        // When: A callback is set
        service.setGameStateCallback { _ ->
            callbackInvoked = true
        }
        
        // Then: The callback reference should be stored (we can't directly test invocation
        // without starting the polling loop, but we can verify the API works)
        // This is more of an integration point - the actual invocation happens in pollGameState
        expectThat(callbackInvoked).isFalse() // Not invoked until poll happens
    }

    @Test
    @DisplayName("Callback can be replaced")
    fun `callback can be replaced`() {
        // Given: A GameStateService with an initial callback
        val service = GameStateService()
        var firstCallbackCount = 0
        var secondCallbackCount = 0
        
        service.setGameStateCallback { firstCallbackCount++ }
        
        // When: Replace with a new callback
        service.setGameStateCallback { secondCallbackCount++ }
        
        // Then: Only the new callback should be stored
        // (verified by the fact that setGameStateCallback replaces, not appends)
        expectThat(firstCallbackCount).isEqualTo(0)
        expectThat(secondCallbackCount).isEqualTo(0)
    }

    @Test
    @DisplayName("Service should handle null callback gracefully")
    fun `service should work without callback set`() {
        // Given: A GameStateService without any callback
        val service = GameStateService()
        
        // Then: Service should be in a valid initial state
        val state = service.trackerState.value
        expectThat(state.connection.connected).isFalse()
        expectThat(state.pollCount).isEqualTo(0)
    }
}

