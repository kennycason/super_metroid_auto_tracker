package com.supermetroid.network

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import strikt.api.expectThat
import strikt.assertions.*

class DualMemoryAdapterTest {
    
    private lateinit var dualAdapter: DualMemoryAdapter
    
    @BeforeEach
    fun setUp() {
        dualAdapter = DualMemoryAdapter()
    }
    
    @Test
    fun `should detect adapter availability`() = runTest {
        val isAvailable = dualAdapter.isAvailable()
        // This test just checks that the detection runs without throwing exceptions
        // The actual result depends on whether RetroArch or SNI services are running
        expectThat(isAvailable).isA<Boolean>()
    }
    
    @Test
    fun `should return correct adapter type`() {
        // When no adapter is connected, it should return a default
        expectThat(dualAdapter.getAdapterType()).isEqualTo(MemoryAdapter.AdapterType.RETROARCH_NWA)
    }
    
    @Test
    fun `should return disconnected state when not connected`() {
        expectThat(dualAdapter.getConnectionState()).isEqualTo(MemoryAdapter.ConnectionState.DISCONNECTED)
    }
    
    @Test
    fun `should return appropriate adapter name when not connected`() {
        expectThat(dualAdapter.getAdapterName()).contains("Not Connected")
    }
    
    @Test
    fun `should provide connection stats`() {
        val stats = dualAdapter.getConnectionStats()
        expectThat(stats.totalRequests).isEqualTo(0)
        expectThat(stats.successfulRequests).isEqualTo(0)
        expectThat(stats.failedRequests).isEqualTo(0)
    }
}
