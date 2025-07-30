package com.supermetroid.model

import kotlinx.serialization.json.Json
import kotlin.test.*

class GameStateTest {
    
    private val json = Json { prettyPrint = true }
    
    @Test
    fun testGameStateSerialization() {
        val gameState = GameState(
            health = 299,
            maxHealth = 399,
            missiles = 50,
            maxMissiles = 230,
            supers = 10,
            maxSupers = 50,
            powerBombs = 5,
            maxPowerBombs = 50,
            roomId = 37368,
            areaId = 1,
            areaName = "Brinstar",
            items = mapOf(
                "morph" to true,
                "bombs" to true,
                "varia" to false
            ),
            beams = mapOf(
                "charge" to true,
                "ice" to true,
                "wave" to false
            ),
            bosses = mapOf(
                "kraid" to true,
                "ridley" to false
            )
        )
        
        // Test serialization
        val jsonString = json.encodeToString(GameState.serializer(), gameState)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"health\": 299"))
        assertTrue(jsonString.contains("\"areaName\": \"Brinstar\""))
        
        // Test deserialization
        val deserializedState = json.decodeFromString(GameState.serializer(), jsonString)
        assertEquals(gameState, deserializedState)
    }
    
    @Test
    fun testGameStateDefaults() {
        val gameState = GameState()
        
        assertEquals(0, gameState.health)
        assertEquals(99, gameState.maxHealth)
        assertEquals(0, gameState.missiles)
        assertEquals("Unknown", gameState.areaName)
        assertEquals(emptyMap(), gameState.items)
        assertEquals(emptyMap(), gameState.beams)
        assertEquals(emptyMap(), gameState.bosses)
    }
    
    @Test
    fun testServerStatusSerialization() {
        val serverStatus = ServerStatus(
            connected = true,
            gameLoaded = true,
            retroarchVersion = "1.15.0",
            gameInfo = "Super Metroid",
            stats = GameState(health = 99, areaName = "Crateria"),
            lastUpdate = 1640995200000L,
            pollCount = 100,
            errorCount = 2
        )
        
        val jsonString = json.encodeToString(ServerStatus.serializer(), serverStatus)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"connected\": true"))
        assertTrue(jsonString.contains("\"retroarchVersion\": \"1.15.0\""))
        
        val deserialized = json.decodeFromString(ServerStatus.serializer(), jsonString)
        assertEquals(serverStatus, deserialized)
    }
    
    @Test
    fun testItemsDataClass() {
        val items = Items(
            morph = true,
            bombs = true,
            varia = false,
            gravity = false,
            hijump = true
        )
        
        val jsonString = json.encodeToString(Items.serializer(), items)
        assertTrue(jsonString.contains("\"morph\": true"))
        assertTrue(jsonString.contains("\"varia\": false"))
        
        val deserialized = json.decodeFromString(Items.serializer(), jsonString)
        assertEquals(items, deserialized)
    }
    
    @Test
    fun testBeamsDataClass() {
        val beams = Beams(
            charge = true,
            ice = true,
            wave = false,
            spazer = false,
            plasma = true,
            hyper = false
        )
        
        val jsonString = json.encodeToString(Beams.serializer(), beams)
        assertTrue(jsonString.contains("\"charge\": true"))
        assertTrue(jsonString.contains("\"plasma\": true"))
        
        val deserialized = json.decodeFromString(Beams.serializer(), jsonString)
        assertEquals(beams, deserialized)
    }
    
    @Test
    fun testBossesDataClass() {
        val bosses = Bosses(
            kraid = true,
            ridley = true,
            phantoon = false,
            draygon = false,
            motherBrain1 = true,
            motherBrain2 = false
        )
        
        val jsonString = json.encodeToString(Bosses.serializer(), bosses)
        assertTrue(jsonString.contains("\"kraid\": true"))
        assertTrue(jsonString.contains("\"motherBrain1\": true"))
        
        val deserialized = json.decodeFromString(Bosses.serializer(), jsonString)
        assertEquals(bosses, deserialized)
    }
    
    @Test
    fun testConnectionInfoDataClass() {
        val connectionInfo = ConnectionInfo(
            connected = true,
            gameLoaded = false,
            retroarchVersion = "1.15.0",
            gameInfo = "No game loaded"
        )
        
        val jsonString = json.encodeToString(ConnectionInfo.serializer(), connectionInfo)
        assertTrue(jsonString.contains("\"connected\": true"))
        assertTrue(jsonString.contains("\"gameLoaded\": false"))
        
        val deserialized = json.decodeFromString(ConnectionInfo.serializer(), jsonString)
        assertEquals(connectionInfo, deserialized)
    }
} 