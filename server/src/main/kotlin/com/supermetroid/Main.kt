package com.supermetroid

import com.supermetroid.server.HttpServer
import kotlinx.coroutines.*
import platform.posix.*

/**
 * Main entry point for Super Metroid Tracker Kotlin Native server
 */
suspend fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull() ?: 8080
    val pollInterval = args.getOrNull(1)?.toLongOrNull() ?: 1000L
    
    println("🎮 Starting Super Metroid Tracker (Kotlin Native)")
    println("Port: $port, Poll Interval: ${pollInterval}ms")
    
    val server = HttpServer(port, pollInterval)
    
    // Handle shutdown signals
    signal(SIGINT) { 
        println("\n🛑 Shutdown requested")
        server.stop()
        exit(0)
    }
    signal(SIGTERM) {
        println("\n🛑 Shutdown requested") 
        server.stop()
        exit(0)
    }
    
    try {
        server.start()
    } catch (e: Exception) {
        println("❌ Server failed to start: ${e.message}")
        server.stop()
        exit(1)
    }
} 