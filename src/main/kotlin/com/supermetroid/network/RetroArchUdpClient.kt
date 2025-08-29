package com.supermetroid.network

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * UDP client for communicating with RetroArch's network command interface
 * Handles memory reading and writing for Super Metroid emulator
 *
 * Features:
 * - Thread-safe memory operations with mutex synchronization
 * - Automatic reconnection on failure
 * - Robust error handling for mixed-up responses
 * - Rate limiting and connection health monitoring
 */
class RetroArchUdpClient(
    private val host: String = "localhost",
    private val port: Int = 55355,
    private val timeoutMs: Int = 5000,
    private val maxRetries: Int = 3
) {
    private var socket: DatagramSocket? = null
    private val address = InetAddress.getByName(host)

    // Mutex for synchronizing UDP operations
    private val udpMutex = Mutex()

    // Connection state tracking
    private var connectionState = ConnectionState.DISCONNECTED
    private var lastErrorTime: Long = 0
    private var consecutiveErrors: Int = 0
    private var totalErrors: Int = 0
    private var totalSuccesses: Int = 0

    // Connection state enum
    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        RECONNECTING
    }

    /**
     * Connect to RetroArch UDP interface
     */
    @Throws(Exception::class)
    fun connect() {
        try {
            // Close any existing socket first
            if (socket != null) {
                disconnect()
            }

            socket = DatagramSocket()
            socket?.soTimeout = timeoutMs
            connectionState = ConnectionState.CONNECTED
            consecutiveErrors = 0
            logger.info { "🔌 Connected to RetroArch UDP at $host:$port" }
        } catch (e: Exception) {
            connectionState = ConnectionState.DISCONNECTED
            logger.error(e) { "❌ Failed to connect to RetroArch UDP" }
            throw e
        }
    }

    /**
     * Disconnect from RetroArch
     */
    fun disconnect() {
        socket?.close()
        socket = null
        connectionState = ConnectionState.DISCONNECTED
        logger.info { "🔌 Disconnected from RetroArch UDP" }
    }

    /**
     * Attempt to reconnect to RetroArch
     * Uses exponential backoff for repeated attempts
     */
    @Throws(Exception::class)
    suspend fun reconnect(): Boolean = withContext(Dispatchers.IO) {
        if (connectionState == ConnectionState.CONNECTED) {
            return@withContext true
        }

        connectionState = ConnectionState.RECONNECTING
        logger.info { "🔄 Attempting to reconnect to RetroArch UDP..." }

        // Calculate backoff delay based on consecutive errors
        val backoffDelayMs = if (consecutiveErrors > 0) {
            val baseDelay = 500L
            val maxDelay = 5000L
            val calculatedDelay = baseDelay * (1 shl minOf(consecutiveErrors - 1, 6)) // Exponential backoff up to 32x
            minOf(calculatedDelay, maxDelay) // Cap at maxDelay
        } else {
            0L // No delay for first attempt
        }

        if (backoffDelayMs > 0) {
            logger.info { "⏱️ Waiting ${backoffDelayMs}ms before reconnect attempt (consecutive errors: $consecutiveErrors)" }
            delay(backoffDelayMs)
        }

        try {
            connect()
            // Reset error counters on successful reconnection
            consecutiveErrors = 0
            logger.info { "✅ Successfully reconnected to RetroArch UDP" }
            return@withContext true
        } catch (e: Exception) {
            connectionState = ConnectionState.DISCONNECTED
            consecutiveErrors++ // Increment consecutive errors on failed reconnect

            // Log more detailed error information
            val errorMessage = e.message ?: "Unknown error"
            val errorType = e.javaClass.simpleName
            logger.error(e) { "❌ Failed to reconnect to RetroArch UDP: $errorType - $errorMessage" }

            // Log connection statistics
            logger.info { "📊 Connection stats: consecutive errors: $consecutiveErrors, total errors: $totalErrors, total successes: $totalSuccesses" }

            return@withContext false
        }
    }

    /**
     * Read memory from specified address
     * Includes automatic reconnection on failure and mutex synchronization
     */
    @Throws(Exception::class)
    suspend fun readMemory(address: Int, size: Int, retryCount: Int = 0): ByteArray = withContext(Dispatchers.IO) {
        // Check connection state and attempt reconnect if needed
        if (connectionState != ConnectionState.CONNECTED) {
            if (retryCount >= maxRetries) {
                throw IllegalStateException("Failed to reconnect to RetroArch after $maxRetries attempts")
            }

            val reconnected = reconnect()
            if (!reconnected) {
                // Increment retry count and try again after a short delay
                delay(100)
                return@withContext readMemory(address, size, retryCount + 1)
            }
        }

        val socket = this@RetroArchUdpClient.socket
            ?: throw IllegalStateException("Not connected to RetroArch")

        val command = "READ_CORE_MEMORY 0x${address.toString(16).uppercase()} $size"
        // Reduce UDP noise
        if (command.hashCode() % 50 == 0) {
            logger.debug { "🔌 Sending command: $command" }
        }

        // Use mutex to ensure only one UDP operation at a time
        try {
            udpMutex.withLock {
                // Send command
                val sendData = command.toByteArray()
                val sendPacket = DatagramPacket(sendData, sendData.size, this@RetroArchUdpClient.address, this@RetroArchUdpClient.port)
                socket.send(sendPacket)

                // Receive response
                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)

                val response = String(receivePacket.data, 0, receivePacket.length)
                // Reduce response noise
                if (response.hashCode() % 50 == 0) {
                    logger.debug { "🔌 Received response: $response" }
                }

                // Update success statistics
                totalSuccesses++
                consecutiveErrors = 0

                // Parse response: "READ_CORE_MEMORY <address> <hex_data>"
                return@withLock parseMemoryResponse(response, size)
            }
        } catch (e: Exception) {
            // Update error statistics
            totalErrors++
            consecutiveErrors++
            lastErrorTime = System.currentTimeMillis()

            // Set connection state to disconnected
            connectionState = ConnectionState.DISCONNECTED

            // Log the error
            logger.error(e) { "❌ Failed to read memory at address 0x${address.toString(16)} (attempt ${retryCount + 1}/$maxRetries)" }

            // Attempt to retry if we haven't exceeded max retries
            if (retryCount < maxRetries) {
                logger.info { "🔄 Retrying read operation (attempt ${retryCount + 1}/$maxRetries)" }
                delay(100L * (retryCount + 1)) // Increasing delay for each retry
                return@withContext readMemory(address, size, retryCount + 1)
            } else {
                throw e
            }
        }
    }

    /**
     * Read multiple memory addresses SEQUENTIALLY to prevent data corruption
     * (concurrent reads were causing memory data to get mixed up)
     */
    suspend fun readMemoryBatch(addresses: Map<String, Pair<Int, Int>>): Map<String, ByteArray> =
        withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, ByteArray>()

            // Read sequentially to ensure data consistency
            for ((key, addressSize) in addresses) {
                try {
                    val data = readMemory(addressSize.first, addressSize.second)
                    results[key] = data

                    logger.debug { "✅ Read $key: ${data.size} bytes from 0x${addressSize.first.toString(16)}" }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Failed to read $key from 0x${addressSize.first.toString(16)}" }
                    throw e
                }
            }

            results
        }

    /**
     * Parse memory response from RetroArch
     * Handles both READ_CORE_MEMORY and WRITE_CORE_MEMORY responses
     */
    private fun parseMemoryResponse(response: String, expectedSize: Int): ByteArray {
        val parts = response.trim().split(" ")

        // Check if this is a WRITE_CORE_MEMORY response mistakenly received by a read operation
        if (parts.size >= 2 && parts[0] == "WRITE_CORE_MEMORY") {
            // Log the issue but don't throw an exception
            logger.warn { "⚠️ Received WRITE_CORE_MEMORY response during read operation: $response" }

            // Return an empty byte array of the expected size
            // This is better than throwing an exception which would crash the operation
            return ByteArray(expectedSize)
        }

        // Normal READ_CORE_MEMORY response validation
        if (parts.size < 3 || parts[0] != "READ_CORE_MEMORY") {
            throw IllegalArgumentException("Invalid memory response format: $response")
        }

        // Extract hex data (everything after address)
        val hexData = parts.drop(2).joinToString("")

        // Convert hex string to byte array
        val bytes = hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        if (bytes.size != expectedSize) {
            logger.warn { "⚠️ Expected $expectedSize bytes, got ${bytes.size}" }
        }

        return bytes
    }

    /**
     * Write memory to specified address
     * Includes automatic reconnection on failure and mutex synchronization
     */
    @Throws(Exception::class)
    suspend fun writeMemory(address: Int, data: ByteArray, retryCount: Int = 0): Boolean = withContext(Dispatchers.IO) {
        // Check connection state and attempt reconnect if needed
        if (connectionState != ConnectionState.CONNECTED) {
            if (retryCount >= maxRetries) {
                throw IllegalStateException("Failed to reconnect to RetroArch after $maxRetries attempts")
            }

            val reconnected = reconnect()
            if (!reconnected) {
                // Increment retry count and try again after a short delay
                delay(100)
                return@withContext writeMemory(address, data, retryCount + 1)
            }
        }

        val socket = this@RetroArchUdpClient.socket
            ?: throw IllegalStateException("Not connected to RetroArch")

        // Convert data to hex string
        val hexData = data.joinToString("") { "%02x".format(it) }

        val command = "WRITE_CORE_MEMORY 0x${address.toString(16).uppercase()} $hexData"

        // Reduce UDP noise - only log occasionally
        if (command.hashCode() % 100 == 0) {
            logger.debug { "🔌 Sending write command: $command" }
        }

        // Use mutex to ensure only one UDP operation at a time
        try {
            udpMutex.withLock {
                // Send command
                val sendData = command.toByteArray()
                val sendPacket = DatagramPacket(sendData, sendData.size, this@RetroArchUdpClient.address, this@RetroArchUdpClient.port)
                socket.send(sendPacket)

                // Receive response
                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)

                val response = String(receivePacket.data, 0, receivePacket.length)

                // Reduce response noise - only log occasionally
                if (response.hashCode() % 100 == 0) {
                    logger.debug { "🔌 Received write response: $response" }
                }

                // Update success statistics
                totalSuccesses++
                consecutiveErrors = 0

                // Check if write was successful
                val success = response.startsWith("WRITE_CORE_MEMORY")
                if (!success) {
                    logger.warn { "⚠️ Write operation returned unsuccessful response: $response" }
                }
                return@withLock success
            }
        } catch (e: Exception) {
            // Update error statistics
            totalErrors++
            consecutiveErrors++
            lastErrorTime = System.currentTimeMillis()

            // Set connection state to disconnected
            connectionState = ConnectionState.DISCONNECTED

            // Log the error
            logger.error(e) { "❌ Failed to write memory at address 0x${address.toString(16)} (attempt ${retryCount + 1}/$maxRetries)" }

            // Attempt to retry if we haven't exceeded max retries
            if (retryCount < maxRetries) {
                logger.info { "🔄 Retrying write operation (attempt ${retryCount + 1}/$maxRetries)" }
                delay(100L * (retryCount + 1)) // Increasing delay for each retry
                return@withContext writeMemory(address, data, retryCount + 1)
            } else {
                throw e
            }
        }
    }

    /**
     * Write a single byte to memory
     */
    suspend fun writeByte(address: Int, value: Byte): Boolean {
        return writeMemory(address, byteArrayOf(value))
    }

    /**
     * Check if connected to RetroArch
     */
    fun isConnected(): Boolean = socket != null && !socket!!.isClosed

    /**
     * Get connection info
     */
    fun getConnectionInfo(): String = if (isConnected()) {
        "Connected to $host:$port"
    } else {
        "Disconnected"
    }
}

/**
 * Memory address constants for Super Metroid
 */
object SuperMetroidAddresses {
    // Player stats
    const val HEALTH = 0x7E09C2
    const val MAX_HEALTH = 0x7E09C4
    const val MISSILES = 0x7E09C6
    const val MAX_MISSILES = 0x7E09C8
    const val SUPERS = 0x7E09CA
    const val MAX_SUPERS = 0x7E09CC
    const val POWER_BOMBS = 0x7E09CE
    const val MAX_POWER_BOMBS = 0x7E09D0
    const val RESERVE_ENERGY = 0x7E09D6
    const val MAX_RESERVE_ENERGY = 0x7E09D4

    // Location
    const val ROOM_ID = 0x7E079B
    const val AREA_ID = 0x7E079F
    const val GAME_STATE = 0x7E0998
    const val PLAYER_X = 0x7E0AF6
    const val PLAYER_Y = 0x7E0AFA

    // Items
    const val COLLECTED_ITEMS = 0x7E09A4
    const val COLLECTED_BEAMS = 0x7E09A8

    // Bosses
    const val BOSS_FLAGS_1 = 0x7ED828
    const val BOSS_FLAGS_2 = 0x7ED829
    const val BOSS_FLAGS_3 = 0x7ED82A
    const val BOSS_FLAGS_4 = 0x7ED82B
    const val BOSS_FLAGS_5 = 0x7ED82C
    const val TOURIAN_BOSSES = 0x7ED82D
    const val CERES_BOSSES = 0x7ED82E
    const val EVENT_FLAGS = 0x7ED821

    // Special
    const val MOTHER_BRAIN_HP = 0x7E0FCC
    const val SHIP_AI = 0x7E0FB2

    // Escape sequence
    const val ESCAPE_TIMER_1 = 0x7E0943
    const val ESCAPE_TIMER_2 = 0x7E0945
    const val ESCAPE_TIMER_3 = 0x7E09E2
    const val ESCAPE_TIMER_4 = 0x7E09E0

    // Palette data (for visual effects)
    const val SAMUS_PALETTE_START = 0x7EC000
    const val ENEMY_PALETTE_1_START = 0x7EC020
    const val ENEMY_PALETTE_2_START = 0x7EC040
    const val ENVIRONMENT_PALETTE_START = 0x7EC060
    const val MAP_ICON_PALETTE_START = 0x7EC080
    const val MAP_AREA_PALETTE_START = 0x7EC0A0
    const val MAP_BACKGROUND_PALETTE_START = 0x7EC0C0
    const val UI_PALETTE_START = 0x7EC0E0
    const val EXTENDED_PALETTE_1_START = 0x7EC100
    const val EXTENDED_PALETTE_2_START = 0x7EC120
    const val HUD_PALETTE_START = 0x7EC140
    const val BEAM_PALETTE_START = 0x7EC160
    const val MISC_PALETTE_START = 0x7EC180
    const val PALETTE_END = 0x7EC200
}

/**
 * Memory reading helper for Super Metroid specific addresses
 */
class SuperMetroidMemoryReader(private val udpClient: RetroArchUdpClient) {

    /**
     * Read all Super Metroid memory addresses in one batch
     */
    suspend fun readAllMemory(): Map<String, ByteArray> {
        val addresses = mapOf(
            "health" to (SuperMetroidAddresses.HEALTH to 2),
            "maxHealth" to (SuperMetroidAddresses.MAX_HEALTH to 2),
            "missiles" to (SuperMetroidAddresses.MISSILES to 2),
            "maxMissiles" to (SuperMetroidAddresses.MAX_MISSILES to 2),
            "supers" to (SuperMetroidAddresses.SUPERS to 2),
            "maxSupers" to (SuperMetroidAddresses.MAX_SUPERS to 2),
            "powerBombs" to (SuperMetroidAddresses.POWER_BOMBS to 2),
            "maxPowerBombs" to (SuperMetroidAddresses.MAX_POWER_BOMBS to 2),
            "reserveEnergy" to (SuperMetroidAddresses.RESERVE_ENERGY to 2),
            "maxReserveEnergy" to (SuperMetroidAddresses.MAX_RESERVE_ENERGY to 2),
            "roomId" to (SuperMetroidAddresses.ROOM_ID to 2),
            "areaId" to (SuperMetroidAddresses.AREA_ID to 1),
            "gameState" to (SuperMetroidAddresses.GAME_STATE to 2),
            "playerX" to (SuperMetroidAddresses.PLAYER_X to 2),
            "playerY" to (SuperMetroidAddresses.PLAYER_Y to 2),
            "collectedItems" to (SuperMetroidAddresses.COLLECTED_ITEMS to 2),
            "collectedBeams" to (SuperMetroidAddresses.COLLECTED_BEAMS to 2),
            "bossFlags1" to (SuperMetroidAddresses.BOSS_FLAGS_1 to 2),
            "bossFlags2" to (SuperMetroidAddresses.BOSS_FLAGS_2 to 2),
            "bossFlags3" to (SuperMetroidAddresses.BOSS_FLAGS_3 to 2),
            "bossFlags4" to (SuperMetroidAddresses.BOSS_FLAGS_4 to 2),
            "bossFlags5" to (SuperMetroidAddresses.BOSS_FLAGS_5 to 2),
            "tourianBosses" to (SuperMetroidAddresses.TOURIAN_BOSSES to 2),
            "ceresBosses" to (SuperMetroidAddresses.CERES_BOSSES to 2),
            "eventFlags" to (SuperMetroidAddresses.EVENT_FLAGS to 2),
            "motherBrainHp" to (SuperMetroidAddresses.MOTHER_BRAIN_HP to 2),
            "shipAi" to (SuperMetroidAddresses.SHIP_AI to 2),
            "escapeTimer1" to (SuperMetroidAddresses.ESCAPE_TIMER_1 to 2),
            "escapeTimer2" to (SuperMetroidAddresses.ESCAPE_TIMER_2 to 2),
            "escapeTimer3" to (SuperMetroidAddresses.ESCAPE_TIMER_3 to 2),
            "escapeTimer4" to (SuperMetroidAddresses.ESCAPE_TIMER_4 to 2)
        )

        return udpClient.readMemoryBatch(addresses)
    }
}

/**
 * Extension functions for ByteArray to read integers
 */
fun ByteArray.readInt16LE(offset: Int = 0): Int {
    if (offset + 1 >= size) return 0
    return (this[offset].toInt() and 0xFF) or
           ((this[offset + 1].toInt() and 0xFF) shl 8)
}

fun ByteArray.readInt8(offset: Int = 0): Int {
    if (offset >= size) return 0
    return this[offset].toInt() and 0xFF
}
