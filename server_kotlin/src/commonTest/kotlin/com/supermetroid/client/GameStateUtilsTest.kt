package com.supermetroid.client

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for GameStateUtils bit parsing functions
 * Tests the logic for determining boss defeats and item possession
 */
class GameStateUtilsTest {

    @Test
    fun testBossDetection() {
        // Test Ridley detection (bit 3 = 0x08)
        assertTrue(GameStateUtils.isRidleyDefeated(0x08u), "Ridley should be defeated with bit 3 set")
        assertTrue(GameStateUtils.isRidleyDefeated(0x0Fu), "Ridley should be defeated with multiple bits set")
        assertFalse(GameStateUtils.isRidleyDefeated(0x07u), "Ridley should not be defeated without bit 3")
        assertFalse(GameStateUtils.isRidleyDefeated(0x00u), "Ridley should not be defeated with no bits set")

        // Test Kraid detection (bit 0 = 0x01) 
        assertTrue(GameStateUtils.isKraidDefeated(0x01u), "Kraid should be defeated with bit 0 set")
        assertTrue(GameStateUtils.isKraidDefeated(0x0Fu), "Kraid should be defeated with multiple bits set")
        assertFalse(GameStateUtils.isKraidDefeated(0x0Eu), "Kraid should not be defeated without bit 0")
        
        // Test Phantoon detection (bit 1 = 0x02)
        assertTrue(GameStateUtils.isPhantoonDefeated(0x02u), "Phantoon should be defeated with bit 1 set")
        assertFalse(GameStateUtils.isPhantoonDefeated(0x0Du), "Phantoon should not be defeated without bit 1")
        
        // Test Draygon detection (bit 2 = 0x04)
        assertTrue(GameStateUtils.isDraygonDefeated(0x04u), "Draygon should be defeated with bit 2 set")
        assertFalse(GameStateUtils.isDraygonDefeated(0x0Bu), "Draygon should not be defeated without bit 2")
    }

    @Test
    fun testItemDetection() {
        // Test Morph Ball (bit 2 = 0x04)
        assertTrue(GameStateUtils.hasMorphBall(0x04u), "Should have Morph Ball with bit 2 set")
        assertTrue(GameStateUtils.hasMorphBall(0x1105u), "Should have Morph Ball with multiple bits set")
        assertFalse(GameStateUtils.hasMorphBall(0x1101u), "Should not have Morph Ball without bit 2")
        
        // Test Varia Suit (bit 0 = 0x01)
        assertTrue(GameStateUtils.hasVariaSuit(0x01u), "Should have Varia Suit with bit 0 set")
        assertTrue(GameStateUtils.hasVariaSuit(0x1105u), "Should have Varia Suit with multiple bits set")
        assertFalse(GameStateUtils.hasVariaSuit(0x1104u), "Should not have Varia Suit without bit 0")
        
        // Test Hi-Jump Boots (bit 8 = 0x100)
        assertTrue(GameStateUtils.hasHiJumpBoots(0x100u), "Should have Hi-Jump Boots with bit 8 set")
        assertTrue(GameStateUtils.hasHiJumpBoots(0x1105u), "Should have Hi-Jump Boots with multiple bits set")
        assertFalse(GameStateUtils.hasHiJumpBoots(0x05u), "Should not have Hi-Jump Boots without bit 8")
    }

    @Test
    fun testBeamDetection() {
        // Test Charge Beam (bit 12 = 0x1000)
        assertTrue(GameStateUtils.hasChargeBeam(0x1000u), "Should have Charge Beam with bit 12 set")
        assertTrue(GameStateUtils.hasChargeBeam(0x1003u), "Should have Charge Beam with multiple bits set")
        assertFalse(GameStateUtils.hasChargeBeam(0x03u), "Should not have Charge Beam without bit 12")
        
        // Test Ice Beam (bit 1 = 0x02)  
        assertTrue(GameStateUtils.hasIceBeam(0x02u), "Should have Ice Beam with bit 1 set")
        assertTrue(GameStateUtils.hasIceBeam(0x1003u), "Should have Ice Beam with multiple bits set")
        assertFalse(GameStateUtils.hasIceBeam(0x1001u), "Should not have Ice Beam without bit 1")
        
        // Test Wave Beam (bit 0 = 0x01)
        assertTrue(GameStateUtils.hasWaveBeam(0x01u), "Should have Wave Beam with bit 0 set")
        assertTrue(GameStateUtils.hasWaveBeam(0x1003u), "Should have Wave Beam with multiple bits set")
        assertFalse(GameStateUtils.hasWaveBeam(0x1002u), "Should not have Wave Beam without bit 0")
    }

    @Test
    fun testPythonReferenceState() {
        // Test values that should match the Python server reference
        // Python reference: morph=true, bombs=true, varia=true, hijump=true
        val pythonItemState = 0x1105u // morph(0x04) + varia(0x01) + hijump(0x100) + bombs(0x1000)
        
        assertTrue(GameStateUtils.hasMorphBall(pythonItemState), "Python ref should have Morph Ball")
        assertTrue(GameStateUtils.hasVariaSuit(pythonItemState), "Python ref should have Varia Suit") 
        assertTrue(GameStateUtils.hasHiJumpBoots(pythonItemState), "Python ref should have Hi-Jump Boots")
        
        // Python reference: charge=true, ice=true, wave=true
        val pythonBeamState = 0x1003u // charge(0x1000) + ice(0x02) + wave(0x01)
        
        assertTrue(GameStateUtils.hasChargeBeam(pythonBeamState), "Python ref should have Charge Beam")
        assertTrue(GameStateUtils.hasIceBeam(pythonBeamState), "Python ref should have Ice Beam")
        assertTrue(GameStateUtils.hasWaveBeam(pythonBeamState), "Python ref should have Wave Beam")
    }
} 