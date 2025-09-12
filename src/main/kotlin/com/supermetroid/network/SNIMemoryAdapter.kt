package com.supermetroid.network

import com.github.alttpo.sni.DeviceMemoryGrpc
import com.github.alttpo.sni.DevicesGrpc
import com.github.alttpo.sni.Sni
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.Socket
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Memory adapter implementation for SNI (Super Nintendo Interface) using gRPC
 * Provides real-time memory reading from SNES devices via SNI service
 */
class SNIMemoryAdapter(
    private val host: String = "localhost",
    private val port: Int = 8191
) : MemoryAdapter {
    
    // gRPC channel and service stubs
    private var channel: ManagedChannel? = null
    private var devicesStub: DevicesGrpc.DevicesBlockingStub? = null
    private var memoryStub: DeviceMemoryGrpc.DeviceMemoryBlockingStub? = null
    
    // Current device URI
    private var currentDeviceUri: String? = null
    
    // Statistics tracking
    private var totalRequests: Long = 0
    private var successfulRequests: Long = 0
    private var failedRequests: Long = 0
    private var lastErrorTime: Long = 0
    private var consecutiveErrors: Int = 0
    private val responseTimes = mutableListOf<Long>()
    
    private var connectionState = MemoryAdapter.ConnectionState.DISCONNECTED
    
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Test if SNI service is running on port 8191
            Socket(host, port).use { socket ->
                socket.soTimeout = 1000 // 1 second timeout
                logger.debug { "✅ SNI service available at $host:$port" }
                true
            }
        } catch (e: Exception) {
            logger.debug { "❌ SNI service not available at $host:$port: ${e.message}" }
            false
        }
    }
    
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            connectionState = MemoryAdapter.ConnectionState.CONNECTING
            
            // Initialize gRPC channel
            channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build()
            
            // Create service stubs
            devicesStub = DevicesGrpc.newBlockingStub(channel)
            memoryStub = DeviceMemoryGrpc.newBlockingStub(channel)
            
            // Try to list devices to verify connection
            val devicesResponse = devicesStub!!.listDevices(Sni.DevicesRequest.getDefaultInstance())
            
            if (devicesResponse.devicesList.isEmpty()) {
                logger.warn { "⚠️ SNI service connected but no devices found" }
                connectionState = MemoryAdapter.ConnectionState.ERROR
                disconnect()
                return@withContext false
            }
            
            // Choose the first available device that supports memory reading
            val device = devicesResponse.devicesList.firstOrNull { device ->
                device.capabilitiesList.contains(Sni.DeviceCapability.ReadMemory)
            }
            
            if (device == null) {
                logger.warn { "⚠️ No devices with ReadMemory capability found" }
                connectionState = MemoryAdapter.ConnectionState.ERROR
                disconnect()
                return@withContext false
            }
            
            currentDeviceUri = device.uri
            connectionState = MemoryAdapter.ConnectionState.CONNECTED
            
            logger.info { "✅ Connected to SNI service at $host:$port with device: ${device.displayName} (${device.kind})" }
            true
        } catch (e: StatusException) {
            connectionState = MemoryAdapter.ConnectionState.ERROR
            logger.error(e) { "❌ gRPC error connecting to SNI service at $host:$port" }
            disconnect()
            false
        } catch (e: Exception) {
            connectionState = MemoryAdapter.ConnectionState.ERROR
            logger.error(e) { "❌ Failed to connect to SNI service at $host:$port" }
            disconnect()
            false
        }
    }
    
    override fun disconnect() {
        try {
            channel?.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.warn(e) { "Error during gRPC channel shutdown" }
        } finally {
            channel = null
            devicesStub = null
            memoryStub = null
            currentDeviceUri = null
            connectionState = MemoryAdapter.ConnectionState.DISCONNECTED
            logger.info { "🔌 Disconnected from SNI service" }
        }
    }
    
    override fun getConnectionState(): MemoryAdapter.ConnectionState {
        return connectionState
    }
    
    override fun getAdapterType(): MemoryAdapter.AdapterType {
        return MemoryAdapter.AdapterType.SNI_GRPC
    }
    
    override fun getAdapterName(): String {
        return "SNI gRPC ($host:$port)"
    }
    
    override suspend fun readMemory(address: Int, size: Int): ByteArray = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        totalRequests++
        
        return@withContext try {
            val memoryStub = this@SNIMemoryAdapter.memoryStub
                ?: throw IllegalStateException("Not connected to SNI service")
            val deviceUri = currentDeviceUri
                ?: throw IllegalStateException("No device selected")
            
            // Create memory read request
            val readRequest = Sni.ReadMemoryRequest.newBuilder()
                .setRequestAddress(address)
                .setRequestAddressSpace(Sni.AddressSpace.SnesABus)
                .setRequestMemoryMapping(Sni.MemoryMapping.LoROM)
                .setSize(size)
                .build()
            
            val singleRequest = Sni.SingleReadMemoryRequest.newBuilder()
                .setUri(deviceUri)
                .setRequest(readRequest)
                .build()
            
            // Execute the request
            val response = memoryStub.singleRead(singleRequest)
            val data = response.response.data.toByteArray()
            
            // Update success statistics
            successfulRequests++
            consecutiveErrors = 0
            val responseTime = System.currentTimeMillis() - startTime
            updateResponseTime(responseTime)
            
            logger.debug { "✅ Read ${data.size} bytes from 0x${address.toString(16)} via SNI" }
            data
        } catch (e: StatusException) {
            // Update error statistics
            failedRequests++
            consecutiveErrors++
            lastErrorTime = System.currentTimeMillis()
            
            logger.error(e) { "❌ SNI gRPC error reading memory at 0x${address.toString(16)}: ${e.status}" }
            throw e
        } catch (e: Exception) {
            // Update error statistics
            failedRequests++
            consecutiveErrors++
            lastErrorTime = System.currentTimeMillis()
            
            logger.error(e) { "❌ Failed to read memory at 0x${address.toString(16)} via SNI" }
            throw e
        }
    }
    
    override suspend fun readMemoryBatch(addresses: Map<String, Pair<Int, Int>>): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        totalRequests++
        
        return@withContext try {
            val memoryStub = this@SNIMemoryAdapter.memoryStub
                ?: throw IllegalStateException("Not connected to SNI service")
            val deviceUri = currentDeviceUri
                ?: throw IllegalStateException("No device selected")
            
            // Create multiple read requests
            val requests = addresses.values.map { (address, size) ->
                Sni.ReadMemoryRequest.newBuilder()
                    .setRequestAddress(address)
                    .setRequestAddressSpace(Sni.AddressSpace.SnesABus)
                    .setRequestMemoryMapping(Sni.MemoryMapping.LoROM)
                    .setSize(size)
                    .build()
            }
            
            val multiRequest = Sni.MultiReadMemoryRequest.newBuilder()
                .setUri(deviceUri)
                .addAllRequests(requests)
                .build()
            
            // Execute the batch request
            val response = memoryStub.multiRead(multiRequest)
            
            // Map responses back to keys
            val results = mutableMapOf<String, ByteArray>()
            addresses.keys.forEachIndexed { index, key ->
                if (index < response.responsesList.size) {
                    results[key] = response.responsesList[index].data.toByteArray()
                }
            }
            
            // Update success statistics
            successfulRequests++
            consecutiveErrors = 0
            val responseTime = System.currentTimeMillis() - startTime
            updateResponseTime(responseTime)
            
            logger.debug { "✅ Batch read ${addresses.size} addresses via SNI" }
            results
        } catch (e: StatusException) {
            // Update error statistics
            failedRequests++
            consecutiveErrors++
            lastErrorTime = System.currentTimeMillis()
            
            logger.error(e) { "❌ SNI gRPC error in batch read: ${e.status}" }
            throw e
        } catch (e: Exception) {
            // Update error statistics
            failedRequests++
            consecutiveErrors++
            lastErrorTime = System.currentTimeMillis()
            
            logger.error(e) { "❌ Failed to batch read memory via SNI" }
            throw e
        }
    }
    
    override suspend fun writeMemory(address: Int, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val memoryStub = this@SNIMemoryAdapter.memoryStub
                ?: throw IllegalStateException("Not connected to SNI service")
            val deviceUri = currentDeviceUri
                ?: throw IllegalStateException("No device selected")
            
            // Create memory write request
            val writeRequest = Sni.WriteMemoryRequest.newBuilder()
                .setRequestAddress(address)
                .setRequestAddressSpace(Sni.AddressSpace.SnesABus)
                .setRequestMemoryMapping(Sni.MemoryMapping.LoROM)
                .setData(com.google.protobuf.ByteString.copyFrom(data))
                .build()
            
            val singleRequest = Sni.SingleWriteMemoryRequest.newBuilder()
                .setUri(deviceUri)
                .setRequest(writeRequest)
                .build()
            
            // Execute the request
            memoryStub.singleWrite(singleRequest)
            
            logger.debug { "✅ Wrote ${data.size} bytes to 0x${address.toString(16)} via SNI" }
            true
        } catch (e: StatusException) {
            logger.error(e) { "❌ SNI gRPC error writing memory at 0x${address.toString(16)}: ${e.status}" }
            false
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to write memory at 0x${address.toString(16)} via SNI" }
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
    
    private fun updateResponseTime(responseTime: Long) {
        responseTimes.add(responseTime)
        // Keep only the last 100 response times for rolling average
        if (responseTimes.size > 100) {
            responseTimes.removeAt(0)
        }
    }
}
