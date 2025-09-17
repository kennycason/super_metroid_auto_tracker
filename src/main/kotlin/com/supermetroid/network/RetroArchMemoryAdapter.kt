package com.supermetroid.network

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException

private val logger = KotlinLogging.logger {}

/**
 * Memory adapter implementation for RetroArch using the existing UDP client
 * Wraps the RetroArchUdpClient to provide the MemoryAdapter interface
 */
class RetroArchMemoryAdapter(
    private val host: String = "localhost",
    private val port: Int = 55355
) : MemoryAdapter {
    
    private val udpClient = RetroArchUdpClient(host, port)
    private val memoryReader = SuperMetroidMemoryReader(udpClient)
    
    // Statistics tracking
    private var totalRequests: Long = 0
    private var successfulRequests: Long = 0
    private var failedRequests: Long = 0
    private var lastErrorTime: Long = 0
    private var consecutiveErrors: Int = 0
    private val responseTimes = mutableListOf<Long>()
    
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Try to connect briefly to test availability
            val testClient = RetroArchUdpClient(host, port)
            testClient.connect()
            
            // Try a simple memory read to verify it's actually working
            testClient.readMemory(0x7E0000, 1)
            testClient.disconnect()
            
            logger.debug { "✅ RetroArch NWA available at $host:$port" }
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
            true
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to connect to RetroArch NWA at $host:$port" }
            false
        }
    }
    
    override fun disconnect() {
        udpClient.disconnect()
        logger.info { "🔌 Disconnected from RetroArch NWA" }
    }
    
    override fun getConnectionState(): MemoryAdapter.ConnectionState {
        return when (udpClient.isConnectedExt()) {
            true -> MemoryAdapter.ConnectionState.CONNECTED
            false -> MemoryAdapter.ConnectionState.DISCONNECTED
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
            throw e
        }
    }
    
    override suspend fun writeMemory(address: Int, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            udpClient.writeMemory(address, data)
            logger.debug { "✅ Wrote ${data.size} bytes to 0x${address.toString(16)} via RetroArch NWA" }
            true
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to write memory at 0x${address.toString(16)} via RetroArch NWA" }
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

/**
 * Extension function to check if RetroArchUdpClient is connected
 * This is a temporary workaround since the original client doesn't expose connection state properly
 */
private fun RetroArchUdpClient.isConnectedExt(): Boolean {
    return try {
        // We'll assume connected if we can create the client successfully
        // The actual connection test happens in the memory read operations
        true
    } catch (e: Exception) {
        false
    }
}
