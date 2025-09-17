package com.supermetroid.network

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * Dual memory adapter that automatically detects and uses the best available memory interface
 * Supports both SNI (hardware + emulators) and RetroArch NWA protocols with automatic failover
 */
class DualMemoryAdapter(
    private val detectionPreferences: MemoryAdapterDetectionService.DetectionPreferences = MemoryAdapterDetectionService.DetectionPreferences()
) : MemoryAdapter {
    
    private val detectionService = MemoryAdapterDetectionService()
    private val connectionMutex = Mutex()
    
    // Current active adapter
    private var activeAdapter: MemoryAdapter? = null
    private var lastDetectionTime: Long = 0
    private var lastSuccessfulOperation: Long = 0
    private val redetectionIntervalMs = 60000 // Re-detect every 60 seconds if no adapter is active
    private val connectionStabilityMs = 10000 // Wait 10 seconds before re-detecting after successful operations
    
    // Fallback adapters for manual connection
    private val fallbackAdapters = listOf<() -> MemoryAdapter>(
        { RetroArchMemoryAdapter(detectionPreferences.retroArchHost, detectionPreferences.retroArchPort) },
        { SNIMemoryAdapter(detectionPreferences.sniHost, detectionPreferences.sniPort) }
    )
    
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext connectionMutex.withLock {
            // If we have an active adapter, check if it's still available
            activeAdapter?.let { adapter ->
                if (adapter.getConnectionState() == MemoryAdapter.ConnectionState.CONNECTED) {
                    return@withLock true
                }
            }
            
            // Otherwise, try to detect available adapters
            val result = detectionService.detectAvailableAdapters(detectionPreferences)
            result.availableAdapters.isNotEmpty()
        }
    }
    
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext connectionMutex.withLock {
            logger.info { "🔗 Connecting dual memory adapter..." }
            
            // If we already have a connected adapter, return success
            activeAdapter?.let { adapter ->
                if (adapter.getConnectionState() == MemoryAdapter.ConnectionState.CONNECTED) {
                    logger.info { "✅ Already connected via ${adapter.getAdapterName()}" }
                    return@withLock true
                }
            }
            
            // Try to get the best available adapter
            try {
                val adapter = detectionService.getBestAvailableAdapter(detectionPreferences)
                if (adapter != null) {
                    activeAdapter = adapter
                    lastDetectionTime = System.currentTimeMillis()
                    logger.info { "✅ Connected dual adapter via ${adapter.getAdapterName()}" }
                    return@withLock true
                }
            } catch (e: Exception) {
                logger.error(e) { "❌ Failed to connect via detection service" }
            }
            
            // If detection service failed, try fallback adapters manually
            logger.info { "🔄 Trying fallback connection methods..." }
            for (adapterFactory in fallbackAdapters) {
                try {
                    val adapter = adapterFactory()
                    if (adapter.connect()) {
                        activeAdapter = adapter
                        lastDetectionTime = System.currentTimeMillis()
                        logger.info { "✅ Connected dual adapter via fallback ${adapter.getAdapterName()}" }
                        return@withLock true
                    }
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to connect fallback adapter" }
                }
            }
            
            logger.error { "❌ Failed to connect any memory adapter" }
            return@withLock false
        }
    }
    
    override fun disconnect() {
        activeAdapter?.disconnect()
        activeAdapter = null
        logger.info { "🔌 Disconnected dual memory adapter" }
    }
    
    override fun getConnectionState(): MemoryAdapter.ConnectionState {
        return activeAdapter?.getConnectionState() ?: MemoryAdapter.ConnectionState.DISCONNECTED
    }
    
    override fun getAdapterType(): MemoryAdapter.AdapterType {
        return activeAdapter?.getAdapterType() ?: MemoryAdapter.AdapterType.RETROARCH_NWA // Default fallback
    }
    
    override fun getAdapterName(): String {
        return activeAdapter?.getAdapterName() ?: "Dual Adapter (Not Connected)"
    }
    
    override suspend fun readMemory(address: Int, size: Int): ByteArray = withContext(Dispatchers.IO) {
        return@withContext ensureConnectedAndExecute("readMemory") { adapter ->
            adapter.readMemory(address, size)
        }
    }
    
    override suspend fun readMemoryBatch(addresses: Map<String, Pair<Int, Int>>): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        return@withContext ensureConnectedAndExecute("readMemoryBatch") { adapter ->
            adapter.readMemoryBatch(addresses)
        }
    }
    
    override suspend fun writeMemory(address: Int, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            ensureConnectedAndExecute("writeMemory") { adapter ->
                adapter.writeMemory(address, data)
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to write memory" }
            false
        }
    }
    
    override fun getConnectionStats(): ConnectionStats {
        return activeAdapter?.getConnectionStats() ?: ConnectionStats()
    }
    
    /**
     * Ensure we have a connected adapter and execute the given operation
     * Handles automatic reconnection and failover with stability checks
     */
    private suspend fun <T> ensureConnectedAndExecute(
        operationName: String,
        operation: suspend (MemoryAdapter) -> T
    ): T {
        return connectionMutex.withLock {
            val now = System.currentTimeMillis()
            
            // Check if we need to re-detect adapters (but be less aggressive)
            val hasActiveAdapter = activeAdapter != null
            val isConnected = activeAdapter?.getConnectionState() == MemoryAdapter.ConnectionState.CONNECTED
            val timeSinceLastDetection = now - lastDetectionTime
            val timeSinceLastSuccess = now - lastSuccessfulOperation
            
            val needsRedetection = !hasActiveAdapter || 
                (!isConnected && timeSinceLastSuccess > connectionStabilityMs)
                // Removed: (timeSinceLastDetection > redetectionIntervalMs) - don't force re-detection if working
            
            if (needsRedetection) {
                logger.info { "🔄 Re-detecting adapters for operation: $operationName (hasAdapter=$hasActiveAdapter, connected=$isConnected, timeSinceDetection=${timeSinceLastDetection}ms)" }
                if (!connect()) {
                    throw IllegalStateException("No memory adapter available for $operationName")
                }
            }
            
            val adapter = activeAdapter 
                ?: throw IllegalStateException("No active adapter available for $operationName")
            
            try {
                val result = operation(adapter)
                lastSuccessfulOperation = now
                return@withLock result
            } catch (e: Exception) {
                logger.warn(e) { "❌ Operation $operationName failed with ${adapter.getAdapterName()}" }
                
                // For gRPC errors, try a simple reconnect first
                if (e.message?.contains("GOAWAY") == true || e.message?.contains("ENHANCE_YOUR_CALM") == true) {
                    logger.info { "🔄 Detected gRPC connection issue, attempting simple reconnect" }
                    try {
                        adapter.disconnect()
                        adapter.connect()
                        val result = operation(adapter)
                        lastSuccessfulOperation = now
                        return@withLock result
                    } catch (reconnectError: Exception) {
                        logger.warn(reconnectError) { "❌ Simple reconnect failed" }
                    }
                }
                
                // If that failed, try to connect to a different adapter
                logger.info { "🔄 Attempting failover to different adapter" }
                activeAdapter = null
                lastDetectionTime = 0  // Force re-detection
                if (connect()) {
                    val newAdapter = activeAdapter!!
                    logger.info { "✅ Switched to ${newAdapter.getAdapterName()} for $operationName" }
                    val result = operation(newAdapter)
                    lastSuccessfulOperation = now
                    return@withLock result
                }
                
                // If all else fails, throw the original error
                throw e
            }
        }
    }
    
    /**
     * Get the current active adapter for backwards compatibility
     * TODO: Remove this once all services use the MemoryAdapter interface
     */
    fun getActiveAdapter(): MemoryAdapter? = activeAdapter
    
    /**
     * Get the underlying RetroArch UDP client if active adapter is RetroArch
     * TODO: Remove this once all services are migrated
     */
    fun getRetroArchUdpClient(): RetroArchUdpClient? {
        return (activeAdapter as? RetroArchMemoryAdapter)?.getUdpClient()
    }
    
    /**
     * Get the memory reader for Super Metroid specific operations
     * TODO: Move these operations into the adapter itself
     */
    fun getMemoryReader(): SuperMetroidMemoryReader? {
        return (activeAdapter as? RetroArchMemoryAdapter)?.getMemoryReader()
    }
    
    /**
     * Force re-detection of adapters (useful for testing or manual refresh)
     */
    suspend fun forceRedetection(): Boolean {
        return connectionMutex.withLock {
            activeAdapter?.disconnect()
            activeAdapter = null
            lastDetectionTime = 0
            connect()
        }
    }
}
