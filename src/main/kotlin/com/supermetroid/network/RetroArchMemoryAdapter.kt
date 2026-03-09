package com.supermetroid.network

import com.supermetroid.util.Logging
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Memory adapter implementation for RetroArch using the existing UDP client
 * Wraps the RetroArchUdpClient to provide the MemoryAdapter interface
 */
class RetroArchMemoryAdapter(
    private val host: String = "localhost",
    private val port: Int = 55355
) : MemoryAdapter, Logging {

    private val udpClient = RetroArchUdpClient(host, port)
    private val memoryReader = SuperMetroidMemoryReader(udpClient)

    // Statistics tracking
    private var totalRequests: Long = 0
    private var successfulRequests: Long = 0
    private var failedRequests: Long = 0
    private var lastErrorTime: Long = 0
    private var consecutiveErrors: Int = 0
    private val responseTimes = mutableListOf<Long>()

    // Track connection state locally to avoid depending on UDP client internals
    private var isConnectedFlag: Boolean = false

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Try to connect briefly to test availability (this logs "Connected" even if nothing is listening!)
            val testClient = RetroArchUdpClient(host, port)
            testClient.connect()

            // CRITICAL: Try a memory read to verify RetroArch is actually running
            // UDP connection succeeds even if nothing is listening, so we MUST test actual communication
            testClient.readMemory(0x7E0000, 1)
            testClient.disconnect()

            logger.debug { "✅ RetroArch NWA available at $host:$port (verified with memory read)" }
            true
        } catch (e: Exception) {
            logger.debug { "❌ RetroArch NWA not available at $host:$port: ${e.message}" }
            false
        }
    }

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            udpClient.connect()
            logger.info { "✅ Connected to RetroArch NWA at $host:$port" }
            isConnectedFlag = true
            true
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to connect to RetroArch NWA at $host:$port" }
            false
        }
    }

    override fun disconnect() {
        udpClient.disconnect()
        isConnectedFlag = false
        logger.info { "🔌 Disconnected from RetroArch NWA" }
    }

    override fun getConnectionState(): MemoryAdapter.ConnectionState {
        return if (isConnectedFlag) {
            MemoryAdapter.ConnectionState.CONNECTED
        } else {
            MemoryAdapter.ConnectionState.DISCONNECTED
        }
    }

    override fun getAdapterType(): MemoryAdapter.AdapterType {
        return MemoryAdapter.AdapterType.RETROARCH_NWA
    }

    override fun getAdapterName(): String {
        return "RetroArch NWA ($host:$port)"
    }

    override suspend fun readMemory(address: Int, size: Int): ByteArray = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        totalRequests++

        return@withContext try {
            val data = udpClient.readMemory(address, size)

            // Update success statistics
            successfulRequests++
            consecutiveErrors = 0
            val responseTime = System.currentTimeMillis() - startTime
            updateResponseTime(responseTime)

            logger.debug { "✅ Read ${data.size} bytes from 0x${address.toString(16)} via RetroArch NWA" }
            data
        } catch (e: Exception) {
            // Update error statistics
            failedRequests++
            consecutiveErrors++
            lastErrorTime = System.currentTimeMillis()

            logger.error(e) { "❌ Failed to read memory at 0x${address.toString(16)} via RetroArch NWA" }
            isConnectedFlag = false
            throw e
        }
    }

    override suspend fun readMemoryBatch(addresses: Map<String, Pair<Int, Int>>): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        totalRequests++

        return@withContext try {
            val results = udpClient.readMemoryBatch(addresses)

            // Update success statistics
            successfulRequests++
            consecutiveErrors = 0
            val responseTime = System.currentTimeMillis() - startTime
            updateResponseTime(responseTime)

            logger.debug { "✅ Batch read ${addresses.size} addresses via RetroArch NWA" }
            results
        } catch (e: Exception) {
            // Update error statistics
            failedRequests++
            consecutiveErrors++
            lastErrorTime = System.currentTimeMillis()

            logger.error(e) { "❌ Failed to batch read memory via RetroArch NWA" }
            isConnectedFlag = false
            throw e
        }
    }

    override suspend fun writeMemory(address: Int, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val success = udpClient.writeMemory(address, data)
            if (success) {
                logger.debug { "✅ Wrote ${data.size} bytes to 0x${address.toString(16)} via RetroArch NWA" }
            } else {
                logger.warn { "⚠️ Write to 0x${address.toString(16)} returned unsuccessful response" }
            }
            success
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to write memory at 0x${address.toString(16)} via RetroArch NWA" }
            isConnectedFlag = false
            false
        }
    }

    override fun getConnectionStats(): ConnectionStats {
        return ConnectionStats(
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            averageResponseTimeMs = if (responseTimes.isNotEmpty()) responseTimes.average() else 0.0,
            lastErrorTime = lastErrorTime,
            consecutiveErrors = consecutiveErrors
        )
    }

    /**
     * Get the underlying UDP client for backwards compatibility
     * TODO: Remove this once all services are migrated to use MemoryAdapter interface
     */
    fun getUdpClient(): RetroArchUdpClient = udpClient

    /**
     * Get the memory reader for Super Metroid specific operations
     * TODO: Move these operations into the adapter itself
     */
    fun getMemoryReader(): SuperMetroidMemoryReader = memoryReader

    private fun updateResponseTime(responseTime: Long) {
        responseTimes.add(responseTime)
        // Keep only the last 100 response times for rolling average
        if (responseTimes.size > 100) {
            responseTimes.removeAt(0)
        }
    }
}
