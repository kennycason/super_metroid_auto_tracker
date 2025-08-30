package com.supermetroid.service

import strikt.api.expectThat
import strikt.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach

/**
 * Tests for logo effects service focusing on state management and effect transitions
 * Critical for ensuring effects don't interfere with each other
 */
class LogoEffectsTest {

    private lateinit var logoEffectsService: LogoEffectsService

    @BeforeEach
    fun setup() {
        logoEffectsService = LogoEffectsService()
    }

    @AfterEach
    fun cleanup() {
        logoEffectsService.stop()
    }

    @Test
    fun `should start with no active effect`() {
        val initialState = logoEffectsService.logoState.value
        
        expectThat(initialState.activeEffect) {
            isEqualTo(LogoEffectType.NONE)
        }
    }

    @Test
    fun `should transition between effect types correctly`() {
        // Given: Start with no effect
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.NONE)
        
        // When: Set to noise effect
        logoEffectsService.setEffectType(LogoEffectType.NOISE)
        
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.NOISE)
        
        // When: Switch to pixel swap
        logoEffectsService.setEffectType(LogoEffectType.PIXEL_SWAP)
        
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.PIXEL_SWAP)
        
        // When: Switch to wave effect
        logoEffectsService.setEffectType(LogoEffectType.WAVE)
        
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.WAVE)
        
        // When: Turn off effects
        logoEffectsService.setEffectType(LogoEffectType.NONE)
        
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.NONE)
    }

    @Test
    fun `should handle tile size changes correctly`() {
        val validTileSizes = listOf(1, 5, 10, 15, 30)
        
        validTileSizes.forEach { size ->
            logoEffectsService.setTileSwapSize(size)
            
            expectThat(logoEffectsService.currentTileSize.value) {
                isEqualTo(size)
            }
        }
    }

    @Test
    fun `should clamp tile sizes to valid range`() {
        val testCases = mapOf(
            "Below minimum" to Pair(-5, 1),
            "At minimum" to Pair(1, 1),
            "Normal value" to Pair(10, 10),
            "At maximum" to Pair(50, 50),
            "Above maximum" to Pair(100, 50)
        )
        
        testCases.forEach { (description, pair) ->
            val (input, expectedOutput) = pair
            logoEffectsService.setTileSwapSize(input)
            
            expectThat(logoEffectsService.currentTileSize.value) {
                isEqualTo(expectedOutput)
            }
        }
    }

    @Test
    fun `should handle service lifecycle correctly`() {
        // Given: Service is started
        logoEffectsService.start()
        
        // Should be able to set effects
        logoEffectsService.setEffectType(LogoEffectType.NOISE)
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.NOISE)
        
        // When: Service is stopped
        logoEffectsService.stop()
        
        // Should reset to no effect
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.NONE)
    }

    @Test
    fun `should reset tracking variables when changing effects`() {
        // This test verifies that internal state is properly reset
        // when switching between effects to prevent cross-contamination
        
        // Given: Start pixel swap effect
        logoEffectsService.setEffectType(LogoEffectType.PIXEL_SWAP)
        
        // When: Switch to different effect
        logoEffectsService.setEffectType(LogoEffectType.WAVE)
        
        // Then: Should not crash or have inconsistent state
        // (Internal tracking variables should be reset)
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.WAVE)
        
        // When: Switch back to pixel swap
        logoEffectsService.setEffectType(LogoEffectType.PIXEL_SWAP)
        
        // Then: Should work correctly (no leftover state from previous)
        expectThat(logoEffectsService.logoState.value.activeEffect)
            .isEqualTo(LogoEffectType.PIXEL_SWAP)
    }

    @Test
    fun `should handle multiple rapid effect changes gracefully`() {
        val effects = listOf(
            LogoEffectType.NOISE,
            LogoEffectType.PIXEL_SWAP,
            LogoEffectType.WAVE,
            LogoEffectType.NONE,
            LogoEffectType.PIXEL_SWAP,
            LogoEffectType.NOISE
        )
        
        // When: Rapidly change effects
        effects.forEach { effect ->
            logoEffectsService.setEffectType(effect)
            
            expectThat(logoEffectsService.logoState.value.activeEffect) {
                isEqualTo(effect)
            }
        }
        
        // Final state should be stable
        expectThat(logoEffectsService.logoState.value.activeEffect) {
            isEqualTo(LogoEffectType.NOISE)
        }
    }
}
