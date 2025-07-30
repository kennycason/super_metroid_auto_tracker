package com.supermetroid.parser

import kotlin.test.*

class BossDetectionTest {
    
    private fun createByteArray(value: Int): ByteArray {
        return byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())
    }
    
    private fun parseTestBosses(
        mainBosses: Int = 0,
        crocomire: Int = 0,
        bossPlus1: Int = 0,
        bossPlus2: Int = 0,
        bossPlus3: Int = 0,
        bossPlus4: Int = 0
    ): Map<String, Boolean> {
        val parser = GameStateParser()
        
        // Call the internal parseBosses function directly (no reflection needed)
        return parser.parseBosses(
            mainBossesData = createByteArray(mainBosses),
            crocomireData = createByteArray(crocomire),
            bossPlus1Data = createByteArray(bossPlus1),
            bossPlus2Data = createByteArray(bossPlus2),
            bossPlus3Data = createByteArray(bossPlus3),
            bossPlus4Data = createByteArray(bossPlus4),
            bossPlus5Data = createByteArray(0),
            escapeTimer1Data = createByteArray(0),
            escapeTimer2Data = createByteArray(0),
            escapeTimer3Data = createByteArray(0),
            escapeTimer4Data = createByteArray(0),
            locationData = emptyMap()
        )
    }
    
    @Test
    fun testBasicBossDetection() {
        // Test Bomb Torizo (bit 2 = 0x0004)
        val bombTorizoResult = parseTestBosses(mainBosses = 0x0004)
        assertTrue(bombTorizoResult["bomb_torizo"]!!, "Bomb Torizo should be detected with bit 2")
        assertFalse(bombTorizoResult["kraid"]!!, "Kraid should not be detected")
        
        // Test Kraid (bit 8 = 0x0100) 
        val kraidResult = parseTestBosses(mainBosses = 0x0100)
        assertTrue(kraidResult["kraid"]!!, "Kraid should be detected with bit 8")
        assertFalse(kraidResult["bomb_torizo"]!!, "Bomb Torizo should not be detected")
        
        // Test Spore Spawn (bit 9 = 0x0200)
        val sporeResult = parseTestBosses(mainBosses = 0x0200)
        assertTrue(sporeResult["spore_spawn"]!!, "Spore Spawn should be detected with bit 9")
        
        // Test Mother Brain (bit 0 = 0x0001)
        val mbResult = parseTestBosses(mainBosses = 0x0001)
        assertTrue(mbResult["mother_brain"]!!, "Mother Brain should be detected with bit 0")
    }
    
    @Test
    fun testCrocomireDetection() {
        // Crocomire requires bit 1 (0x0002) AND value >= 0x0202
        val crocomireResult1 = parseTestBosses(crocomire = 0x0202)
        assertTrue(crocomireResult1["crocomire"]!!, "Crocomire should be detected with 0x0202")
        
        // Should NOT detect with just bit 1
        val crocomireResult2 = parseTestBosses(crocomire = 0x0002)
        assertFalse(crocomireResult2["crocomire"]!!, "Crocomire should NOT be detected with just 0x0002")
        
        // Should NOT detect without bit 1 even if >= 0x0202
        val crocomireResult3 = parseTestBosses(crocomire = 0x0200)
        assertFalse(crocomireResult3["crocomire"]!!, "Crocomire should NOT be detected without bit 1")
    }
    
    @Test
    fun testDraygonDetection() {
        // CRITICAL: Draygon should ONLY be detected with boss_plus_3 == 0x0301
        val draygonCorrect = parseTestBosses(bossPlus3 = 0x0301)
        assertTrue(draygonCorrect["draygon"]!!, "Draygon should be detected with boss_plus_3 = 0x0301")
        
        // Should NOT be detected with mainBosses bit 12 (0x1000) - this was the bug!
        val draygonBug = parseTestBosses(mainBosses = 0x1000)
        assertFalse(draygonBug["draygon"]!!, "Draygon should NOT be detected with mainBosses bit 12 (Speed Booster conflict)")
        
        // Should NOT be detected with other boss_plus_3 values
        val draygonWrong1 = parseTestBosses(bossPlus3 = 0x0300)
        assertFalse(draygonWrong1["draygon"]!!, "Draygon should NOT be detected with boss_plus_3 = 0x0300")
        
        val draygonWrong2 = parseTestBosses(bossPlus3 = 0x0001)
        assertFalse(draygonWrong2["draygon"]!!, "Draygon should NOT be detected with boss_plus_3 = 0x0001")
    }
    
    @Test
    fun testPhantoonDetection() {
        // Phantoon requires boss_plus_3 != 0 AND bit 0 set
        val phantoonCorrect = parseTestBosses(bossPlus3 = 0x0301)
        assertTrue(phantoonCorrect["phantoon"]!!, "Phantoon should be detected with boss_plus_3 = 0x0301 (bit 0 set)")
        
        val phantoonCorrect2 = parseTestBosses(bossPlus3 = 0x0001)
        assertTrue(phantoonCorrect2["phantoon"]!!, "Phantoon should be detected with boss_plus_3 = 0x0001")
        
        // Should NOT be detected without bit 0
        val phantoonWrong = parseTestBosses(bossPlus3 = 0x0300)
        assertFalse(phantoonWrong["phantoon"]!!, "Phantoon should NOT be detected without bit 0")
        
        // Should NOT be detected with boss_plus_3 = 0
        val phantoonZero = parseTestBosses(bossPlus3 = 0x0000)
        assertFalse(phantoonZero["phantoon"]!!, "Phantoon should NOT be detected with boss_plus_3 = 0")
    }
    
    @Test
    fun testBotwoonDetection() {
        // Botwoon condition 1: boss_plus_2 has bit 2 (0x04) AND > 0x0100
        val botwoonCondition1 = parseTestBosses(bossPlus2 = 0x0104)
        assertTrue(botwoonCondition1["botwoon"]!!, "Botwoon should be detected with boss_plus_2 = 0x0104")
        
        // Botwoon condition 2: boss_plus_4 has bit 1 (0x02) AND > 0x0001
        val botwoonCondition2 = parseTestBosses(bossPlus4 = 0x0003)
        assertTrue(botwoonCondition2["botwoon"]!!, "Botwoon should be detected with boss_plus_4 = 0x0003")
        
        // Should NOT be detected with boss_plus_2 = 0x0004 (bit set but not > 0x0100)
        val botwoonWrong1 = parseTestBosses(bossPlus2 = 0x0004)
        assertFalse(botwoonWrong1["botwoon"]!!, "Botwoon should NOT be detected with boss_plus_2 = 0x0004")
        
        // Should NOT be detected with boss_plus_4 = 0x0002 (bit set but not > 0x0001)
        val botwoonWrong2 = parseTestBosses(bossPlus4 = 0x0002)
        assertFalse(botwoonWrong2["botwoon"]!!, "Botwoon should NOT be detected with boss_plus_4 = 0x0002")
    }
    
    @Test
    fun testSpeedBoosterDraygonConflict() {
        // This tests the SPECIFIC bug we fixed - Speed Booster uses bit patterns that 
        // were incorrectly triggering Draygon detection in the old logic
        
        // Simulate having Speed Booster (which might set mainBosses bit 12 = 0x1000)
        val speedBoosterState = parseTestBosses(mainBosses = 0x1000)
        
        // Draygon should be FALSE even with mainBosses bit 12 set
        assertFalse(speedBoosterState["draygon"]!!, 
            "CRITICAL: Draygon should NOT be detected when Speed Booster bit is set (mainBosses = 0x1000)")
        
        // Only the specific boss_plus_3 = 0x0301 pattern should trigger Draygon
        val realDraygonState = parseTestBosses(
            mainBosses = 0x1000,  // Speed Booster active
            bossPlus3 = 0x0301    // Real Draygon pattern
        )
        assertTrue(realDraygonState["draygon"]!!, 
            "Draygon should be detected with correct boss_plus_3 pattern even when Speed Booster is active")
    }
    
    @Test
    fun testComplexScenario() {
        // Test a realistic game state: multiple bosses defeated
        val gameState = parseTestBosses(
            mainBosses = 0x0205,  // Bomb Torizo (0x0004) + Spore Spawn (0x0200) + Mother Brain (0x0001) = 0x0205
            crocomire = 0x0202,   // Crocomire defeated
            bossPlus3 = 0x0301    // Draygon defeated  
        )
        
        assertTrue(gameState["bomb_torizo"]!!, "Bomb Torizo should be detected")
        assertTrue(gameState["spore_spawn"]!!, "Spore Spawn should be detected")
        assertTrue(gameState["mother_brain"]!!, "Mother Brain should be detected")
        assertTrue(gameState["crocomire"]!!, "Crocomire should be detected")
        assertTrue(gameState["draygon"]!!, "Draygon should be detected")
        assertTrue(gameState["phantoon"]!!, "Phantoon should be detected (same pattern as Draygon)")
        
        assertFalse(gameState["kraid"]!!, "Kraid should NOT be detected")
        assertFalse(gameState["botwoon"]!!, "Botwoon should NOT be detected")
    }
} 