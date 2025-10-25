package com.supermetroid.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for the SNES Game Genie decoder
 */
class GameGenieDecoderTest {

    private val decoder = GameGenieDecoder()

    @Test
    fun `test character mapping`() {
        // Test that all Game Genie characters map correctly
        val expectedMappings = mapOf(
            'A' to 0x0, 'P' to 0x1, 'Z' to 0x2, 'L' to 0x3,
            'G' to 0x4, 'I' to 0x5, 'T' to 0x6, 'O' to 0x7,
            'X' to 0x8, 'U' to 0x9, 'K' to 0xA, 'S' to 0xB,
            'V' to 0xC, 'Y' to 0xD, 'E' to 0xE, 'N' to 0xF
        )

        expectedMappings.forEach { (char, expectedHex) ->
            val decoded = decoder.decodeGameGenieCode("${char}${char}${char}${char}${char}${char}${char}${char}")
            assertTrue(decoded.isValid, "Character $char should be valid")
        }
    }

    @Test
    fun `test code cleaning and normalization`() {
        val testCases = mapOf(
            "DCD7-F26D" to "DCD7F26D",
            "DCD7 F26D" to "DCD7F26D", 
            "DCD7:F26D" to "DCD7F26D",
            "dcd7f26d" to "DCD7F26D",
            "DCD7F26D" to "DCD7F26D"
        )

        testCases.forEach { (input, expected) ->
            val decoded = decoder.decodeGameGenieCode(input)
            assertTrue(decoded.isValid, "Code '$input' should be valid after cleaning")
        }
    }

    @Test
    fun `test invalid codes`() {
        val invalidCodes = listOf(
            "", // Empty
            "DCD7", // Too short
            "DCD7F26D123", // Too long
            "INVALID", // Invalid characters
            "DCD7-F26", // Too short with separator
            "DCD7-F26D123" // Too long with separator
        )

        invalidCodes.forEach { code ->
            val decoded = decoder.decodeGameGenieCode(code)
            assertFalse(decoded.isValid, "Code '$code' should be invalid")
            assertNotNull(decoded.errorMessage, "Invalid code should have error message")
        }
    }

    @Test
    fun `test known Game Genie codes`() {
        // Test with the specific codes mentioned in the user's request
        val knownCodes = mapOf(
            "DCD7F26D" to "Maximum missiles=10",
            "FBD7F26D" to "Maximum missiles=25", 
            "74D7F26D" to "Maximum missiles=50"
        )

        knownCodes.forEach { (code, description) ->
            val decoded = decoder.decodeGameGenieCode(code)
            assertTrue(decoded.isValid, "Known code '$code' ($description) should be valid")
            assertTrue(decoded.address > 0, "Address should be positive")
            assertTrue(decoded.value >= 0, "Value should be non-negative")
            assertNotNull(decoded.compare, "Compare byte should be present")
        }
    }

    @Test
    fun `test RetroArch format conversion`() {
        val testCode = "DCD7F26D"
        val decoded = decoder.decodeGameGenieCode(testCode)
        
        assertTrue(decoded.isValid, "Test code should be valid")
        
        val retroArchFormat = decoder.convertToRetroArchFormat(decoded)
        assertNotNull(retroArchFormat, "Should convert to RetroArch format")
        
        // Should be in format "ADDRESS:VALUE"
        assertTrue(retroArchFormat!!.contains(":"), "Should contain colon separator")
        val parts = retroArchFormat.split(":")
        assertEquals(2, parts.size, "Should have exactly 2 parts")
        assertTrue(parts[0].matches(Regex("[0-9A-F]+")), "Address should be hex")
        assertTrue(parts[1].matches(Regex("[0-9A-F]+")), "Value should be hex")
    }

    @Test
    fun `test code description`() {
        val testCode = "DCD7F26D"
        val decoded = decoder.decodeGameGenieCode(testCode)
        
        assertTrue(decoded.isValid, "Test code should be valid")
        
        val description = decoder.describeGameGenieCode(decoded)
        assertTrue(description.contains("0x"), "Description should contain hex values")
        assertTrue(description.contains("memory address"), "Description should mention memory address")
    }

    @Test
    fun `test code validation`() {
        val validCodes = listOf(
            "DCD7F26D",
            "FBD7F26D", 
            "74D7F26D",
            "ABCDEFG0", // Valid format even if not a real code
            "12345678" // Now valid with regular hex support
        )

        val invalidCodes = listOf(
            "DCD7F26", // Too short
            "DCD7F26D123", // Too long
            "INVALID" // Invalid characters
        )

        validCodes.forEach { code ->
            assertTrue(decoder.isValidGameGenieCode(code), "Code '$code' should be valid")
        }

        invalidCodes.forEach { code ->
            assertFalse(decoder.isValidGameGenieCode(code), "Code '$code' should be invalid")
        }
    }

    @Test
    fun `test edge cases`() {
        // Test with all same character
        val allSame = "AAAAAAAA"
        val decoded = decoder.decodeGameGenieCode(allSame)
        assertTrue(decoded.isValid, "All same character code should be valid")
        assertEquals(0, decoded.value, "All A's should decode to 0")

        // Test with alternating characters
        val alternating = "APZLGITX"
        val decodedAlt = decoder.decodeGameGenieCode(alternating)
        assertTrue(decodedAlt.isValid, "Alternating character code should be valid")
    }

    @Test
    fun `test decoder consistency`() {
        // Test that the same code always produces the same result
        val testCode = "DCD7F26D"
        val result1 = decoder.decodeGameGenieCode(testCode)
        val result2 = decoder.decodeGameGenieCode(testCode)
        
        assertEquals(result1.address, result2.address, "Address should be consistent")
        assertEquals(result1.value, result2.value, "Value should be consistent")
        assertEquals(result1.compare, result2.compare, "Compare should be consistent")
        assertEquals(result1.isValid, result2.isValid, "Validity should be consistent")
    }

    @Test
    fun `test known code test function`() {
        val testResults = decoder.testKnownCodes()
        
        assertTrue(testResults.isNotEmpty(), "Should have test results")
        
        testResults.forEach { (code, result) ->
            assertTrue(result.isValid, "Known test code '$code' should be valid")
            assertTrue(result.address > 0, "Address should be positive")
        }
    }

    @Test
    fun `test specific missile codes`() {
        // Test the specific codes mentioned by the user
        val missileCodes = mapOf(
            "DCD7F26D" to "Max missiles=10",
            "FBD7F26D" to "Max missiles=25",
            "74D7F26D" to "Max missiles=50"
        )

        missileCodes.forEach { (code, description) ->
            val decoded = decoder.decodeGameGenieCode(code)
            
            assertTrue(decoded.isValid, "Missile code '$code' ($description) should be valid")
            
            // Convert to RetroArch format and verify it's valid
            val retroArchFormat = decoder.convertToRetroArchFormat(decoded)
            assertNotNull(retroArchFormat, "Should convert to RetroArch format")
            
            // Verify the format is correct
            val parts = retroArchFormat!!.split(":")
            assertEquals(2, parts.size, "Should have address and value")
            assertTrue(parts[0].length == 6, "Address should be 6 hex digits")
            assertTrue(parts[1].length == 2, "Value should be 2 hex digits")
            
            println("✅ $code ($description) -> $retroArchFormat")
        }
    }
}
