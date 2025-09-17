package com.supermetroid.network

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import strikt.api.expectThat
import strikt.assertions.*

class SNIMemoryAdapterTest {
    
    private lateinit var sniAdapter: SNIMemoryAdapter
    
    @BeforeEach
    fun setUp() {
        sniAdapter = SNIMemoryAdapter()
    }
    
    @Test
    @Disabled("Requires SNI service to be running - use for manual testing only")
    fun `should detect SNI availability`() = runTest {
        val isAvailable = sniAdapter.isAvailable()
        // This test just checks that the detection runs without throwing exceptions
        // The actual result depends on whether SNI service is running
        expectThat(isAvailable).isA<Boolean>()
    }
    
    @Test
    fun `should return correct adapter type`() {
        expectThat(sniAdapter.getAdapterType()).isEqualTo(MemoryAdapter.AdapterType.SNI_GRPC)
    }
    
    @Test
    fun `should return disconnected state when not connected`() {
        expectThat(sniAdapter.getConnectionState()).isEqualTo(MemoryAdapter.ConnectionState.DISCONNECTED)
    }
    
    @Test
    fun `should return appropriate adapter name`() {
        expectThat(sniAdapter.getAdapterName()).contains("SNI gRPC")
    }
    
    @Test
    fun `should provide connection stats`() {
        val stats = sniAdapter.getConnectionStats()
        expectThat(stats.totalRequests).isEqualTo(0)
        expectThat(stats.successfulRequests).isEqualTo(0)
        expectThat(stats.failedRequests).isEqualTo(0)
    }
    
    @Test
    @Disabled("Requires SNI service to be running - use for manual testing only")
    fun `should handle connection failure gracefully when SNI service is not running`() = runTest {
        val connected = sniAdapter.connect()
        // This will likely fail since SNI is probably not running in CI
        // But it should fail gracefully without throwing exceptions
        expectThat(connected).isA<Boolean>()
        
        // Ensure we can disconnect safely even if connection failed
        sniAdapter.disconnect()
        expectThat(sniAdapter.getConnectionState()).isEqualTo(MemoryAdapter.ConnectionState.DISCONNECTED)
    }
}
