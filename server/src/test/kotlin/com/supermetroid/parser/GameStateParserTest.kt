package com.supermetroid.parser

import com.supermetroid.model.GameState
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class GameStateParserTest {
    
    private val parser = GameStateParser()
    
    @Test
    fun testParseBasicStats() = runTest {
        // Create mock basic stats data (22 bytes)
        val basicStatsData = byteArrayOf(
            0x63, 0x00, // health = 99
            0x63, 0x00, // max_health = 99
            0x00, 0x00, // missiles = 0
            0x05, 0x00, // max_missiles = 5
            0x00, 0x00, // supers = 0
            0x00, 0x00, // max_supers = 0
            0x00, 0x00, // power_bombs = 0
            0x00, 0x00, // max_power_bombs = 0
            0x00, 0x00, // max_reserve_energy = 0
            0x00, 0x00  // reserve_energy = 0
        )
        
        val memoryData = mapOf("basic_stats" to basicStatsData)
        val gameState = parser.parseCompleteGameState(memoryData)
        
        assertEquals(99, gameState.health)
        assertEquals(99, gameState.maxHealth)
        assertEquals(0, gameState.missiles)
        assertEquals(5, gameState.maxMissiles)
    }
    
    @Test
    fun testParseItemsNewGame() = runTest {
        // Test that items are reset in new game scenario
        val basicStatsData = byteArrayOf(
            0x63, 0x00, // health = 99
            0x63, 0x00, // max_health = 99
            0x00, 0x00, // missiles = 0
            0x00, 0x00, // max_missiles = 0
            *ByteArray(14) { 0 } // Fill rest with zeros
        )
        
        val roomIdData = byteArrayOf(0x64, 0x01) // room = 356 (< 1000)
        val areaIdData = byteArrayOf(0x00) // area = 0 (Crateria)
        val itemsData = byteArrayOf(0xFF.toByte(), 0xFF.toByte()) // All items set (should be reset)
        
        val memoryData = mapOf(
            "basic_stats" to basicStatsData,
            "room_id" to roomIdData,
            "area_id" to areaIdData,
            "items" to itemsData
        )
        
        val gameState = parser.parseCompleteGameState(memoryData)
        
        // Should reset all items to false despite memory having all items
        assertFalse(gameState.items["morph"] ?: true)
        assertFalse(gameState.items["bombs"] ?: true)
        assertFalse(gameState.items["varia"] ?: true)
    }
    
    @Test
    fun testParseItemsNormalGame() = runTest {
        // Test normal item parsing when not in new game scenario
        val basicStatsData = byteArrayOf(
            0x2C, 0x01, // health = 300
            0x2C, 0x01, // max_health = 300
            0x14, 0x00, // missiles = 20
            0x14, 0x00, // max_missiles = 20
            *ByteArray(14) { 0 } // Fill rest with zeros
        )
        
        val roomIdData = byteArrayOf(0x00, 0x20) // room = 8192 (> 1000)
        val areaIdData = byteArrayOf(0x01) // area = 1 (Brinstar)
        val itemsData = byteArrayOf(0x05, 0x00) // morph (0x04) + varia (0x01) = 0x05
        
        val memoryData = mapOf(
            "basic_stats" to basicStatsData,
            "room_id" to roomIdData,
            "area_id" to areaIdData,
            "items" to itemsData
        )
        
        val gameState = parser.parseCompleteGameState(memoryData)
        
        // Should parse actual item values
        assertTrue(gameState.items["morph"] ?: false)
        assertTrue(gameState.items["varia"] ?: false)
        assertFalse(gameState.items["bombs"] ?: true)
    }
    
    @Test
    fun testParseBeamsNewGame() = runTest {
        // Test that beams are reset in new game scenario (including charge beam)
        val basicStatsData = byteArrayOf(
            0x63, 0x00, // health = 99
            0x63, 0x00, // max_health = 99
            0x00, 0x00, // missiles = 0
            0x00, 0x00, // max_missiles = 0
            *ByteArray(14) { 0 } // Fill rest with zeros
        )
        
        val roomIdData = byteArrayOf(0x64, 0x01) // room = 356 (< 1000)
        val areaIdData = byteArrayOf(0x00) // area = 0 (Crateria)
        val beamsData = byteArrayOf(0xFF.toByte(), 0xFF.toByte()) // All beams set (should be reset)
        
        val memoryData = mapOf(
            "basic_stats" to basicStatsData,
            "room_id" to roomIdData,
            "area_id" to areaIdData,
            "beams" to beamsData
        )
        
        val gameState = parser.parseCompleteGameState(memoryData)
        
        // Should reset all beams to false, including charge beam
        assertFalse(gameState.beams["charge"] ?: true)
        assertFalse(gameState.beams["ice"] ?: true)
        assertFalse(gameState.beams["wave"] ?: true)
        assertFalse(gameState.beams["plasma"] ?: true)
        assertFalse(gameState.beams["hyper"] ?: true)
    }
    
    @Test
    fun testParseBeamsNormalGame() = runTest {
        // Test normal beam parsing
        val basicStatsData = byteArrayOf(
            0x2C, 0x01, // health = 300
            0x2C, 0x01, // max_health = 300
            0x14, 0x00, // missiles = 20
            0x14, 0x00, // max_missiles = 20
            *ByteArray(14) { 0 } // Fill rest with zeros
        )
        
        val roomIdData = byteArrayOf(0x00, 0x20) // room = 8192 (> 1000)
        val areaIdData = byteArrayOf(0x01) // area = 1 (Brinstar)
        val beamsData = byteArrayOf(0x03, 0x10) // wave (0x01) + ice (0x02) + charge (0x1000) = 0x1003
        
        val memoryData = mapOf(
            "basic_stats" to basicStatsData,
            "room_id" to roomIdData,
            "area_id" to areaIdData,
            "beams" to beamsData
        )
        
        val gameState = parser.parseCompleteGameState(memoryData)
        
        // Should parse actual beam values
        assertTrue(gameState.beams["charge"] ?: false) // 0x1000 bit
        assertTrue(gameState.beams["ice"] ?: false) // 0x02 bit
        assertTrue(gameState.beams["wave"] ?: false) // 0x01 bit
        assertFalse(gameState.beams["plasma"] ?: true) // Not set
    }
    
    @Test
    fun testParseBosses() = runTest {
        // Test basic boss parsing
        val mainBossesData = byteArrayOf(0x07, 0x00) // kraid (0x01) + spore spawn (0x02) + bomb torizo (0x04) = 0x07
        
        val memoryData = mapOf(
            "main_bosses" to mainBossesData
        )
        
        val gameState = parser.parseCompleteGameState(memoryData)
        
        assertTrue(gameState.bosses["kraid"] ?: false)
        assertTrue(gameState.bosses["spore_spawn"] ?: false)
        assertTrue(gameState.bosses["bomb_torizo"] ?: false)
        assertFalse(gameState.bosses["crocomire"] ?: true)
        assertFalse(gameState.bosses["ridley"] ?: true)
    }
    
    @Test
    fun testParseLocationData() = runTest {
        // Test location parsing
        val roomIdData = byteArrayOf(0x91, 0x91) // Little-endian room ID
        val areaIdData = byteArrayOf(0x02) // Norfair
        val gameStateData = byteArrayOf(0x08, 0x00)
        val playerXData = byteArrayOf(0x00, 0x01)
        val playerYData = byteArrayOf(0x80, 0x00)
        
        val memoryData = mapOf(
            "room_id" to roomIdData,
            "area_id" to areaIdData,
            "game_state" to gameStateData,
            "player_x" to playerXData,
            "player_y" to playerYData
        )
        
        val gameState = parser.parseCompleteGameState(memoryData)
        
        assertEquals(0x9191, gameState.roomId)
        assertEquals(2, gameState.areaId)
        assertEquals("Norfair", gameState.areaName)
        assertEquals(8, gameState.gameState)
        assertEquals(256, gameState.playerX)
        assertEquals(128, gameState.playerY)
    }
    
    @Test
    fun testMotherBrainCacheReset() = runTest {
        // Test MB cache reset functionality
        parser.resetMotherBrainCache()
        
        val memoryData = mapOf<String, ByteArray>()
        val gameState = parser.parseCompleteGameState(memoryData)
        
        assertFalse(gameState.bosses["mother_brain_1"] ?: true)
        assertFalse(gameState.bosses["mother_brain_2"] ?: true)
    }
    
    @Test
    fun testEmptyMemoryData() = runTest {
        // Test with empty memory data
        val memoryData = mapOf<String, ByteArray>()
        val gameState = parser.parseCompleteGameState(memoryData)
        
        assertEquals(GameState(), gameState)
    }
    
    @Test
    fun testIncompleteMemoryData() = runTest {
        // Test with incomplete memory data
        val basicStatsData = byteArrayOf(0x63, 0x00) // Only 2 bytes instead of 22
        val memoryData = mapOf("basic_stats" to basicStatsData)
        
        val gameState = parser.parseCompleteGameState(memoryData)
        
        // Should handle gracefully and return default values
        assertEquals(0, gameState.health)
        assertEquals(0, gameState.maxHealth)
    }
} 