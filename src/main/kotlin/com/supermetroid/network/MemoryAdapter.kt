package com.supermetroid.network

/**
 * Common interface for reading memory from different SNES devices/emulators
 * Supports both SNI (hardware + emulators) and RetroArch NWA protocols
 */
interface MemoryAdapter {
    
    /**
     * Connection state for the memory adapter
     */
    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        ERROR
    }
    
    /**
     * Type of adapter implementation
     */
    enum class AdapterType {
        RETROARCH_NWA,
        SNI_GRPC
    }
    
    /**
     * Check if the adapter can connect to its target service
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Connect to the memory service
     */
    suspend fun connect(): Boolean
    
    /**
     * Disconnect from the memory service
     */
    fun disconnect()
    
    /**
     * Get current connection state
     */
    fun getConnectionState(): ConnectionState
    
    /**
     * Get adapter type
     */
    fun getAdapterType(): AdapterType
    
    /**
     * Get adapter name for display/logging
     */
    fun getAdapterName(): String
    
    /**
     * Read memory from specified address
     * @param address Memory address to read from
     * @param size Number of bytes to read
     * @return ByteArray containing the memory data
     */
    suspend fun readMemory(address: Int, size: Int): ByteArray
    
    /**
     * Read multiple memory addresses in batch for efficiency
     * @param addresses Map of identifier -> (address, size) pairs
     * @return Map of identifier -> data pairs
     */
    suspend fun readMemoryBatch(addresses: Map<String, Pair<Int, Int>>): Map<String, ByteArray>
    
    /**
     * Write memory to specified address (optional - not all adapters may support this)
     * @param address Memory address to write to
     * @param data Data to write
     */
    suspend fun writeMemory(address: Int, data: ByteArray): Boolean {
        // Default implementation - not supported
        return false
    }
    
    /**
     * Get connection statistics for monitoring
     */
    fun getConnectionStats(): ConnectionStats
}

/**
 * Connection statistics for monitoring adapter health
 */
data class ConnectionStats(
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0,
    val averageResponseTimeMs: Double = 0.0,
    val lastErrorTime: Long = 0,
    val consecutiveErrors: Int = 0
) {
    val successRate: Double
        get() = if (totalRequests > 0) (successfulRequests.toDouble() / totalRequests * 100) else 0.0
}
