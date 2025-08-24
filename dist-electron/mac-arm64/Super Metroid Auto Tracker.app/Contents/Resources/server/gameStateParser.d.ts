import type { GameState, MemoryData } from './types';
/**
 * Super Metroid Game State Parser
 * Converts raw memory data into structured game state
 * Equivalent to the Kotlin GameStateParser
 */
export declare class GameStateParser {
    private motherBrainPhaseState;
    private readonly areas;
    /**
     * Parse complete game state from memory data
     */
    parseCompleteGameState(memoryData: MemoryData): GameState;
    /**
     * Parse basic stats from memory data
     */
    private parseBasicStats;
    /**
     * Parse location data
     */
    private parseLocationData;
    /**
     * Check if we should reset item state (new game detection)
     */
    private shouldResetItemState;
    /**
     * Parse items from memory data
     */
    private parseItems;
    /**
     * Parse beams from memory data
     */
    private parseBeams;
    /**
     * Parse bosses from memory data - Advanced detection using multiple memory addresses
     * Converted from working Kotlin GameStateParser.kt
     */
    private parseBosses;
    /**
     * Reset Mother Brain cache
     */
    resetMotherBrainCache(): void;
    /**
     * Detect when Samus has reached her ship (end-game completion)
     * Based on the Kotlin implementation's detectSamusShip method
     */
    private detectSamusShip;
    /**
     * Read 16-bit little-endian integer from Uint8Array
     */
    private readInt16LE;
    /**
     * Get empty game state
     */
    private getEmptyGameState;
}
//# sourceMappingURL=gameStateParser.d.ts.map