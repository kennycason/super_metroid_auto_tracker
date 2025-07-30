package com.supermetroid.client

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.cinterop.*
import platform.posix.*

/**
 * RetroArch UDP client using platform.posix sockets
 * Simplified implementation based on the working Python version
 */
class RetroArchUDPClient(
    private val host: String = "localhost",
    private val port: Int = 55355
) {
    private val mutex = Mutex()
    private val timeoutSeconds = 1 // 1 second timeout, same as Python

    /**
     * Send a command to RetroArch and get the response
     * This method creates a new socket for each command, similar to the Python implementation
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun sendCommand(command: String): String? = mutex.withLock {
        println("🔌 UDP: Sending command '$command' to $host:$port")

        var socketFd = -1

        try {
            memScoped {
                // Create a new socket for each command (like Python does)
                socketFd = socket(AF_INET, SOCK_DGRAM, 0)
                if (socketFd < 0) {
                    println("❌ UDP: Failed to create socket")
                    return null
                }
                println("🔌 UDP: Socket created successfully with fd=$socketFd")

                // Set socket timeout (like Python's sock.settimeout(1.0))
                val timeout = alloc<timeval>()
                timeout.tv_sec = timeoutSeconds.toLong()
                timeout.tv_usec = 0

                val result = setsockopt(
                    socketFd,
                    SOL_SOCKET,
                    SO_RCVTIMEO,
                    timeout.ptr.reinterpret<ByteVar>(),
                    sizeOf<timeval>().convert()
                )

                if (result < 0) {
                    println("⚠️ UDP: Failed to set socket timeout, continuing anyway")
                }

                // Convert command to bytes
                val commandBytes = command.encodeToByteArray()

                // Setup destination address
                val addr = alloc<sockaddr_in>()
                addr.sin_family = AF_INET.convert()
                addr.sin_port = ((port and 0xFF) shl 8 or (port shr 8)).convert() // Network byte order
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

                println("🔌 UDP: Successfully sent $bytesSent bytes")

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
                println("🔌 UDP: Received $bytesReceived bytes. Response: $response")
                return response
            }
        } catch (e: Exception) {
            println("❌ UDP: Error for command $command: ${e.message}")
            return null
        } finally {
            // Always close the socket when done (like Python does)
            if (socketFd != -1) {
                close(socketFd)
                println("🔌 UDP: Socket closed")
            }
        }
    }

    /**
     * Read a range of memory from RetroArch
     * This method sends a READ_CORE_MEMORY command and parses the response
     */
    suspend fun readMemoryRange(startAddress: Int, size: Int): ByteArray? {
        println("🔌 UDP: Reading memory range 0x${startAddress.toString(16).uppercase()} (size: $size)")

        val command = "READ_CORE_MEMORY 0x${startAddress.toString(16).uppercase()} $size"
        println("🔌 UDP: Sending memory read command: $command")

        val response = sendCommand(command)
        if (response == null) {
            println("❌ UDP: Failed to read memory range - no response received")
            return null
        }

        if (!response.startsWith("READ_CORE_MEMORY")) {
            println("❌ UDP: Invalid response format: $response")
            return null
        }

        return try {
            println("🔌 UDP: Parsing memory data from response...")
            val parts = response.split(' ', limit = 3)
            if (parts.size < 3) {
                println("❌ UDP: Invalid response format (not enough parts): $response")
                return null
            }

            val hexData = parts[2].replace(" ", "")
            println("🔌 UDP: Hex data: $hexData")

            if (hexData.isEmpty()) {
                println("❌ UDP: Empty hex data in response")
                return null
            }

            val byteArray = hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            println("🔌 UDP: Successfully parsed ${byteArray.size} bytes of memory data")

            byteArray
        } catch (e: Exception) {
            println("❌ UDP: Failed to parse memory data: ${e.message}")
            null
        }
    }

    /**
     * Get information about RetroArch and the loaded game
     * This method sends VERSION and GET_STATUS commands and combines the results
     */
    suspend fun getRetroArchInfo(): Map<String, Any> {
        println("🔌 UDP: Getting RetroArch info...")

        println("🔌 UDP: Checking RetroArch version...")
        val versionResponse = sendCommand("VERSION")
        println("🔌 UDP: Version response: $versionResponse")

        println("🔌 UDP: Checking game status...")
        val gameInfoResponse = sendCommand("GET_STATUS")
        println("🔌 UDP: Game info response: $gameInfoResponse")

        val connected = versionResponse != null
        val gameLoaded = gameInfoResponse?.contains("PLAYING") == true

        println("🔌 UDP: RetroArch info summary:")
        println("🔌 UDP: - Connected: $connected")
        println("🔌 UDP: - RetroArch version: ${versionResponse ?: "Unknown"}")
        println("🔌 UDP: - Game loaded: $gameLoaded")
        println("🔌 UDP: - Game info: ${gameInfoResponse ?: "No game loaded"}")

        return buildMap {
            put("connected", connected)
            put("retroarch_version", versionResponse ?: "Unknown")
            put("game_loaded", gameLoaded)
            put("game_info", gameInfoResponse ?: "No game loaded")
        }
    }
}
