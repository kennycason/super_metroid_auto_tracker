package com.supermetroid.client

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import platform.posix.*
import kotlinx.cinterop.*

/**
 * RetroArch UDP client for reading game memory
 * Equivalent to Python's RetroArchUDPReader
 */
class RetroArchUDPClient(
    private val host: String = "localhost",
    private val port: Int = 55355
) {
    private val mutex = Mutex()
    private var socket: ConnectedDatagramSocket? = null
    private val timeoutMs = 1000L
    
    suspend fun connect(): Boolean = mutex.withLock {
        try {
            socket?.close()
            
            val selectorManager = SelectorManager()
            val datagramSocket = aSocket(selectorManager).udp().connect(
                InetSocketAddress(host, port)
            )
            socket = datagramSocket
            return true
        } catch (e: Exception) {
            println("UDP connection failed: ${e.message}")
            return false
        }
    }
    
    suspend fun sendCommand(command: String): String? = mutex.withLock {
        val currentSocket = socket ?: run {
            if (!connect()) return null
            socket
        } ?: return null
        
        return try {
            withTimeout(timeoutMs) {
                // Clear any pending data first
                try {
                    withTimeout(100) {
                        currentSocket.incoming.receive()
                    }
                } catch (_: TimeoutCancellationException) {
                    // Expected - no pending data
                }
                
                // Send command
                val packet = Datagram(
                    packet = buildPacket { writeText(command) },
                    address = InetSocketAddress(host, port)
                )
                currentSocket.outgoing.send(packet)
                
                // Receive response
                val response = currentSocket.incoming.receive()
                response.packet.readText()
            }
        } catch (e: TimeoutCancellationException) {
            println("UDP timeout for command: $command")
            null
        } catch (e: Exception) {
            println("UDP error for command $command: ${e.message}")
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
    
    fun close() {
        socket?.close()
        socket = null
    }
} 