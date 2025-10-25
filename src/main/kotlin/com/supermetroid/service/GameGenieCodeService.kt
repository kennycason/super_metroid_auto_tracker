package com.supermetroid.service

import com.supermetroid.network.RetroArchUdpClient
import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for applying Game Genie codes to RetroArch
 */
class GameGenieCodeService(
    private val host: String = "localhost",
    private val port: Int = 55355
) : Logging {
    
    private val udpClient = RetroArchUdpClient(host, port)
    
    /**
     * Apply a Game Genie code to the emulator
     * Format: "7E0998:00FF" (address:value)
     */
    suspend fun applyGameGenieCode(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "🎮 Applying Game Genie code: $code" }
            
            // Parse the code (format: "address:value")
            val parts = code.split(":")
            if (parts.size != 2) {
                logger.error { "❌ Invalid Game Genie code format: $code (expected format: address:value)" }
                return@withContext false
            }
            
            val address = parts[0].trim()
            val value = parts[1].trim()
            
            // Validate hex format
            if (!address.matches(Regex("[0-9A-Fa-f]+")) || !value.matches(Regex("[0-9A-Fa-f]+"))) {
                logger.error { "❌ Invalid hex format in Game Genie code: $code" }
                return@withContext false
            }
            
            // Connect to RetroArch
            udpClient.connect()
            
            // Convert hex value to byte array
            val valueBytes = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val addressInt = address.toInt(16)
            
            logger.debug { "📤 Writing to address 0x${addressInt.toString(16)}: ${valueBytes.joinToString("") { "%02x".format(it) }}" }
            
            // Write the value to memory
            val success = udpClient.writeMemory(addressInt, valueBytes)
            
            if (success) {
                logger.info { "✅ Game Genie code applied successfully: $code" }
            } else {
                logger.warn { "⚠️ Game Genie code may not have been applied: $code" }
            }
            
            udpClient.disconnect()
            return@withContext success
            
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to apply Game Genie code: $code" }
            return@withContext false
        }
    }
    
    /**
     * Apply multiple Game Genie codes in sequence
     */
    suspend fun applyGameGenieCodes(codes: List<String>): Boolean = withContext(Dispatchers.IO) {
        logger.info { "🎮 Applying ${codes.size} Game Genie codes" }
        
        var allSuccess = true
        for (code in codes) {
            val success = applyGameGenieCode(code)
            if (!success) {
                allSuccess = false
                logger.warn { "⚠️ Failed to apply code: $code" }
            }
            
            // Small delay between codes to avoid overwhelming RetroArch
            kotlinx.coroutines.delay(100)
        }
        
        if (allSuccess) {
            logger.info { "✅ All Game Genie codes applied successfully" }
        } else {
            logger.warn { "⚠️ Some Game Genie codes failed to apply" }
        }
        
        return@withContext allSuccess
    }
    
    /**
     * Test connection to RetroArch
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            udpClient.connect()
            val data = udpClient.readMemory(0x7E0998, 1)
            udpClient.disconnect()
            
            val success = data.isNotEmpty()
            logger.info { if (success) "✅ Game Genie service connected to RetroArch" else "❌ Game Genie service connection failed" }
            return@withContext success
            
        } catch (e: Exception) {
            logger.error(e) { "❌ Game Genie service connection test failed" }
            return@withContext false
        }
    }
}
