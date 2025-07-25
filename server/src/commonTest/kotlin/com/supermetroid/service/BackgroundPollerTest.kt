package com.supermetroid.service

import com.supermetroid.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlin.test.*

class BackgroundPollerTest {
    
    @Test
    fun testPollerInitialization() = runTest {
        val poller = BackgroundPoller(updateInterval = 100L)
        
        val initialState = poller.getCachedState()
        
        assertEquals(ServerStatus(), initialState)
        assertFalse(initialState.connected)
        assertFalse(initialState.gameLoaded)
        assertEquals(0, initialState.pollCount)
        assertEquals(0, initialState.errorCount)
    }
    
    @Test
    fun testCacheReset() = runTest {
        val poller = BackgroundPoller(updateInterval = 100L)
        
        // Reset cache should work even without starting
        poller.resetCache()
        
        val state = poller.getCachedState()
        assertEquals(ServerStatus(), state)
    }
    
    @Test
    fun testPollerStartStop() = runTest {
        val poller = BackgroundPoller(updateInterval = 100L)
        
        // Start poller
        poller.start()
        
        // Give it a moment to initialize
        delay(50)
        
        // Stop poller
        poller.stop()
        
        // Should not throw any exceptions
        assertTrue(true)
    }
    
    @Test
    fun testConcurrentCacheAccess() = runTest {
        val poller = BackgroundPoller(updateInterval = 100L)
        
        // Test that concurrent access to cache doesn't cause issues
        repeat(10) {
            launch {
                val state = poller.getCachedState()
                assertNotNull(state)
            }
        }
        
        repeat(5) {
            launch {
                poller.resetCache()
            }
        }
        
        // All operations should complete without exceptions
        assertTrue(true)
    }
    
    @Test
    fun testUpdateInterval() {
        val shortInterval = BackgroundPoller(updateInterval = 100L)
        val longInterval = BackgroundPoller(updateInterval = 5000L)
        
        // Just test that different intervals are accepted
        assertNotNull(shortInterval)
        assertNotNull(longInterval)
    }
} 