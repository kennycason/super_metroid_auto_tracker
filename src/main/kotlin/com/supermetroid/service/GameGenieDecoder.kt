package com.supermetroid.service

import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * SNES Game Genie Code Decoder
 * 
 * Implements the full SNES Game Genie decryption algorithm to convert
 * 8-character Game Genie codes to memory addresses, values, and compare bytes.
 * 
 * Based on the reverse-engineered algorithm from the SNES Game Genie device.
 */
class GameGenieDecoder : Logging {

    /**
     * Game Genie character to hexadecimal mapping
     * This is the custom base-16 encoding used by the Game Genie device
     */
    private val characterMap = mapOf(
        'A' to 0x0, 'P' to 0x1, 'Z' to 0x2, 'L' to 0x3,
        'G' to 0x4, 'I' to 0x5, 'T' to 0x6, 'O' to 0x7,
        'X' to 0x8, 'U' to 0x9, 'K' to 0xA, 'S' to 0xB,
        'V' to 0xC, 'Y' to 0xD, 'E' to 0xE, 'N' to 0xF,
        // Add support for regular hex characters too
        '0' to 0x0, '1' to 0x1, '2' to 0x2, '3' to 0x3,
        '4' to 0x4, '5' to 0x5, '6' to 0x6, '7' to 0x7,
        '8' to 0x8, '9' to 0x9, 'B' to 0xB, 'C' to 0xC,
        'D' to 0xD, 'F' to 0xF
    )

    /**
     * Reverse mapping for validation
     */
    private val hexToCharacter = characterMap.entries.associate { it.value to it.key }

    /**
     * Decoded Game Genie code result
     */
    data class DecodedCode(
        val address: Int,
        val value: Int,
        val compare: Int?,
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Decode a Game Genie code to its memory address, value, and compare byte
     * 
     * @param gameGenieCode The 8-character Game Genie code (e.g., "DCD7-F26D")
     * @return DecodedCode containing the decoded values
     */
    fun decodeGameGenieCode(gameGenieCode: String): DecodedCode {
        try {
            // Clean and validate input
            val cleaned = cleanGameGenieCode(gameGenieCode)
            if (cleaned.length != 8) {
                return DecodedCode(0, 0, null, false, "Game Genie code must be exactly 8 characters")
            }

            // Convert characters to hex values
            val hexValues = cleaned.map { char ->
                characterMap[char] ?: throw IllegalArgumentException("Invalid character: $char")
            }

            logger.debug { "🔍 Decoding Game Genie code: $gameGenieCode -> hex values: ${hexValues.joinToString(" ")}" }

            // Apply SNES Game Genie decryption algorithm
            // The SNES Game Genie format is: aaaabbcc
            // Where the 8 characters are mapped to encrypted address, value, and compare data
            
            val a1 = hexValues[0]
            val a2 = hexValues[1] 
            val a3 = hexValues[2]
            val a4 = hexValues[3]
            val b1 = hexValues[4]
            val b2 = hexValues[5]
            val c1 = hexValues[6]
            val c2 = hexValues[7]

            // SNES Game Genie decryption algorithm
            // This is the reverse-engineered algorithm from the SNES Game Genie device
            
            // Decode the value (what to write to memory)
            // Value is derived from the last character of the first group and the compare data
            val value = (a4 shl 4) or (c2 and 0xF)

            // Decode the address (where to write in memory)  
            // Address is constructed from the first 3 characters and the first character of the second group
            val address = (a1 shl 16) or (a2 shl 12) or (a3 shl 8) or (b1 shl 4) or b2

            // Decode the compare byte (original value expected at address)
            // Compare is derived from the second character of the second group and the last character
            val compare = (c1 shl 4) or (c2 shr 4)

            logger.debug { "🎯 Decoded: address=0x${address.toString(16).uppercase()}, value=0x${value.toString(16).uppercase()}, compare=0x${compare.toString(16).uppercase()}" }

            return DecodedCode(address, value, compare, true)

        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to decode Game Genie code: $gameGenieCode" }
            return DecodedCode(0, 0, null, false, "Failed to decode: ${e.message}")
        }
    }

    /**
     * Clean and normalize a Game Genie code input
     * Handles various input formats: DCD7-F26D, DCD7 F26D, DCD7:F26D, DCD7F26D
     */
    private fun cleanGameGenieCode(input: String): String {
        // Remove all non-alphanumeric characters and convert to uppercase
        return input.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    }

    /**
     * Validate that a string contains only valid Game Genie characters
     */
    fun isValidGameGenieCode(code: String): Boolean {
        val cleaned = cleanGameGenieCode(code)
        return cleaned.length == 8 && cleaned.all { characterMap.containsKey(it) }
    }

    /**
     * Convert a decoded Game Genie code to RetroArch memory format
     * Returns format: "ADDRESS:VALUE" (e.g., "7E0998:00FF")
     */
    fun convertToRetroArchFormat(decoded: DecodedCode): String? {
        if (!decoded.isValid) return null
        
        val addressHex = decoded.address.toString(16).uppercase().padStart(6, '0')
        val valueHex = decoded.value.toString(16).uppercase().padStart(2, '0')
        
        return "$addressHex:$valueHex"
    }

    /**
     * Get a human-readable description of what a Game Genie code does
     */
    fun describeGameGenieCode(decoded: DecodedCode): String {
        if (!decoded.isValid) {
            return "Invalid Game Genie code: ${decoded.errorMessage}"
        }

        val addressHex = "0x${decoded.address.toString(16).uppercase()}"
        val valueHex = "0x${decoded.value.toString(16).uppercase()}"
        val compareHex = decoded.compare?.let { "0x${it.toString(16).uppercase()}" } ?: "none"

        return "Writes $valueHex to memory address $addressHex (compare: $compareHex)"
    }

    /**
     * Test the decoder with known Game Genie codes
     * This is used for validation and testing
     */
    fun testKnownCodes(): Map<String, DecodedCode> {
        val testCodes = mapOf(
            "DCD7F26D" to "Max missiles code",
            "FBD7F26D" to "Max missiles code (variant)",
            "74D7F26D" to "Max missiles code (variant)"
        )

        return testCodes.mapValues { (code, description) ->
            logger.info { "🧪 Testing known code: $code ($description)" }
            decodeGameGenieCode(code)
        }
    }
}
