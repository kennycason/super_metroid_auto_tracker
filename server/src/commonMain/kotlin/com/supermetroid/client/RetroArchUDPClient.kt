package com.supermetroid.client

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.cinterop.*
import platform.posix.*

/**
 * RetroArch UDP client using platform.posix sockets
 * Direct socket implementation to match Python's approach exactly
 */
class RetroArchUDPClient(
    private val host: String = "localhost",
    private val port: Int = 55355
) {
    private val mutex = Mutex()
    private var socketFd: Int = -1
    private val timeoutMs = 1000L
    
    @OptIn(ExperimentalForeignApi::class)
    suspend fun connect(): Boolean = mutex.withLock {
        try {
            // Close existing socket
            if (socketFd != -1) {
                close(socketFd)
                socketFd = -1
            }
            
            // Create UDP socket (just like Python: socket.socket(AF_INET, SOCK_DGRAM))
            socketFd = socket(AF_INET, SOCK_DGRAM, 0)
            if (socketFd < 0) {
                println("❌ Failed to create socket")
                return false
            }
            
            // Set timeout (just like Python: sock.settimeout(1.0))
            memScoped {
                val timeout = alloc<timeval>()
                timeout.tv_sec = 1
                timeout.tv_usec = 0
                
                setsockopt(socketFd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())
                setsockopt(socketFd, SOL_SOCKET, SO_SNDTIMEO, timeout.ptr, sizeOf<timeval>().convert())
            }
            
            println("🔌 UDP: Socket ready for $host:$port")
            return true
        } catch (e: Exception) {
            println("❌ UDP connection failed: ${e.message}")
            return false
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    suspend fun sendCommand(command: String): String? = mutex.withLock {
        println("🔌 UDP: Sending command '$command' to $host:$port")
        
        if (socketFd == -1) {
            println("🔌 UDP: No socket, attempting to connect...")
            if (!connect()) {
                println("❌ UDP: Connection failed")
                return null
            }
        }
        
        return try {
            memScoped {
                // Clear any pending data first (like Python does)
                try {
                    val clearBuffer = ByteArray(1024)
                    while (true) {
                        val result = recv(socketFd, clearBuffer.refTo(0), clearBuffer.size.convert(), MSG_DONTWAIT)
                        if (result <= 0) break
                    }
                } catch (e: Exception) {
                    // Expected - no pending data
                }
                
                // Convert command to bytes
                val commandBytes = command.encodeToByteArray()
                
                // Setup destination address (simplified)
                val addr = alloc<sockaddr_in>()
                addr.sin_family = AF_INET.convert()
                addr.sin_port = ((port and 0xFF) shl 8 or (port shr 8)).convert() // Correct network byte order
                addr.sin_addr.s_addr = 0x0100007F.convert() // 127.0.0.1 in network byte order
                
                // Send command (like Python: sock.sendto(command.encode(), (host, port)))
                val bytesSent = sendto(
                    socketFd,
                    commandBytes.refTo(0),
                    commandBytes.size.convert(),
                    0,
                    addr.ptr.reinterpret(),
                    sizeOf<sockaddr_in>().convert()
                )
                
                if (bytesSent < 0) {
                    println("❌ UDP: Failed to send command")
                    return null
                }
                
                println("🔌 UDP: Sent $bytesSent bytes")
                
                // Receive response (like Python: data, addr = sock.recvfrom(1024))
                val buffer = ByteArray(1024)
                val bytesReceived = recvfrom(
                    socketFd,
                    buffer.refTo(0),
                    buffer.size.convert(),
                    0,
                    null,
                    null
                )
                
                if (bytesReceived < 0) {
                    println("⏰ UDP: Receive timeout or error")
                    return null
                }
                
                val response = buffer.decodeToString(0, bytesReceived.toInt()).trim()
                println("🔌 UDP: Received: $response")
                response
            }
        } catch (e: Exception) {
            println("❌ UDP error for command $command: ${e.message}")
            null
        }
    }

    suspend fun readMemoryRange(startAddress: Int, size: Int): ByteArray? {
        val command = "READ_CORE_MEMORY 0x${startAddress.toString(16).uppercase()} $size"
        val response = sendCommand(command) ?: return null
        
        if (!response.startsWith("READ_CORE_MEMORY")) {
            return null
        }
        
        return try {
            val parts = response.split(' ', limit = 3)
            if (parts.size < 3) return null
            
            val hexData = parts[2].replace(" ", "")
            hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            println("Failed to parse memory data: ${e.message}")
            null
        }
    }
    
    suspend fun getRetroArchInfo(): Map<String, Any> {
        val versionResponse = sendCommand("VERSION")
        val gameInfoResponse = sendCommand("GET_STATUS")
        
        return buildMap {
            put("connected", versionResponse != null)
            put("retroarch_version", versionResponse ?: "Unknown")
            put("game_loaded", gameInfoResponse?.contains("PLAYING") == true)
            put("game_info", gameInfoResponse ?: "No game loaded")
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    fun close() {
        if (socketFd != -1) {
            close(socketFd)
            socketFd = -1
        }
    }
} 