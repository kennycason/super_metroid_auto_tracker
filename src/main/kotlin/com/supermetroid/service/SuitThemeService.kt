package com.supermetroid.service

import com.supermetroid.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Service for managing dynamic suit-based visual themes
 * Responds to Samus's suit state to provide immersive visual effects
 */
class SuitThemeService {
    
    private val _currentSuitTheme = MutableStateFlow(SuitTheme.POWER_SUIT)
    val currentSuitTheme: StateFlow<SuitTheme> = _currentSuitTheme.asStateFlow()
    
    private val _visualEffectsEnabled = MutableStateFlow(false)
    val visualEffectsEnabled: StateFlow<Boolean> = _visualEffectsEnabled.asStateFlow()
    
    private val _glowIntensity = MutableStateFlow(0.7f)
    val glowIntensity: StateFlow<Float> = _glowIntensity.asStateFlow()
    
    private val _animationSpeed = MutableStateFlow(1.0f)
    val animationSpeed: StateFlow<Float> = _animationSpeed.asStateFlow()
    
    /**
     * Update suit theme based on current game state
     */
    fun updateSuitTheme(gameState: GameState) {
        val newTheme = determineSuitTheme(gameState)
        
        // Always update to ensure consistency
        _currentSuitTheme.value = newTheme
    }
    
    private fun determineSuitTheme(gameState: GameState): SuitTheme {
        return when {
            // Gravity Suit - Purple/Blue evolving theme
            gameState.items.gravity -> SuitTheme.GRAVITY_SUIT
            
            // Varia Suit - Orange/Red theme
            gameState.items.varia -> SuitTheme.VARIA_SUIT
            
            // Power Suit (default) - Blue theme
            else -> SuitTheme.POWER_SUIT
        }
    }
    
    suspend fun setVisualEffectsEnabled(enabled: Boolean) {
        _visualEffectsEnabled.value = enabled
        logger.info { "🎨 Visual effects ${if (enabled) "enabled" else "disabled"}" }
    }
    
    suspend fun setGlowIntensity(intensity: Float) {
        _glowIntensity.value = intensity.coerceIn(0.1f, 1.0f)
        logger.info { "🎨 Glow intensity set to ${_glowIntensity.value}" }
    }
    
    suspend fun setAnimationSpeed(speed: Float) {
        _animationSpeed.value = speed.coerceIn(0.1f, 3.0f)
        logger.info { "🎨 Animation speed set to ${_animationSpeed.value}" }
    }
}

/**
 * Suit-based visual themes with glow effects
 */
enum class SuitTheme(
    val displayName: String,
    val primaryColor: String,
    val glowColor: String,
    val description: String
) {
    POWER_SUIT(
        displayName = "Power Suit",
        primaryColor = "#4A90E2", // Blue
        glowColor = "#6BB6FF", // Light Blue
        description = "Standard blue Power Suit theme"
    ),
    
    VARIA_SUIT(
        displayName = "Varia Suit", 
        primaryColor = "#FF6B35", // Orange
        glowColor = "#FF8C42", // Light Orange
        description = "Orange Varia Suit with heat resistance glow"
    ),
    
    GRAVITY_SUIT(
        displayName = "Gravity Suit",
        primaryColor = "#8A2BE2", // Purple
        glowColor = "#A855F7", // Light Purple
        description = "Purple Gravity Suit with evolving blue-purple glow"
    )
}
