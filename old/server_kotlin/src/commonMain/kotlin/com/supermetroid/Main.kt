import com.supermetroid.server.HttpServer
import kotlinx.coroutines.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import platform.posix.*

/**
 * Main entry point for Super Metroid Tracker Kotlin Native server
 */
@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull() ?: 8080
    val pollInterval = args.getOrNull(1)?.toLongOrNull() ?: 1000L
    
    println("🎮 Starting Super Metroid Tracker (Kotlin Native)")
    println("Port: $port, Poll Interval: ${pollInterval}ms")
    
    val server = HttpServer(port, pollInterval)
    
    // Handle shutdown signals
    signal(SIGINT, staticCFunction<Int, Unit> { 
        println("\n🛑 Shutdown requested")
        exit(0)
    })
    signal(SIGTERM, staticCFunction<Int, Unit> {
        println("\n🛑 Shutdown requested") 
        exit(0)
    })
    
    runBlocking {
        try {
            server.start()
        } catch (e: Exception) {
            println("❌ Server failed to start: ${e.message}")
            server.stop()
            exit(1)
        }
    }
} 