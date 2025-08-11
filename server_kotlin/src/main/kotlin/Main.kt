import kotlinx.coroutines.runBlocking
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Super Metroid Grappling Beam Enabler
 * This program writes to Super Metroid's memory to give Samus the grappling beam
 * Standalone version that uses JVM-compatible libraries
 */
fun main() = runBlocking {
    println("🔧 Super Metroid Grappling Beam Enabler (Standalone)")

    // Create a simple UDP client
    val retroArchHost = "localhost"
    val retroArchPort = 55355

    // Check if RetroArch is running and Super Metroid is loaded
    println("🔌 Checking if RetroArch is running...")
    val versionResponse = sendUdpCommand("VERSION", retroArchHost, retroArchPort)
    if (versionResponse == null) {
        println("❌ Error: RetroArch is not running")
        return@runBlocking
    }

    println("🔌 Checking if a game is loaded...")
    val statusResponse = sendUdpCommand("GET_STATUS", retroArchHost, retroArchPort)
    if (statusResponse == null || !statusResponse.contains("PLAYING")) {
        println("❌ Error: No game is loaded in RetroArch")
        return@runBlocking
    }

    println("✅ Connected to RetroArch")
    println("🎮 RetroArch version: $versionResponse")
    println("🎮 Game info: $statusResponse")

    // First, read the current items value
    println("📖 Reading current items data...")
    val itemsAddress = 0x7E09A4
    val currentItemsData = readMemoryRange(itemsAddress, 2, retroArchHost, retroArchPort)

    if (currentItemsData == null || currentItemsData.size < 2) {
        println("❌ Error: Failed to read items data")
        return@runBlocking
    }

    // Convert bytes to int (little-endian)
    val currentItems = (currentItemsData[0].toInt() and 0xFF) or
                       ((currentItemsData[1].toInt() and 0xFF) shl 8)

    println("📊 Current items value: 0x${currentItems.toString(16).uppercase()}")

    // Set the grappling beam bit (0x4000)
    val newItems = currentItems or 0x4000
    println("📊 New items value: 0x${newItems.toString(16).uppercase()}")

    // Convert back to bytes (little-endian)
    val newItemsData = byteArrayOf(
        (newItems and 0xFF).toByte(),
        ((newItems shr 8) and 0xFF).toByte()
    )

    // Write the new value to memory
    println("✏️ Writing grappling beam to memory...")
    val writeCommand = "WRITE_CORE_MEMORY 0x${itemsAddress.toString(16).uppercase()} ${newItemsData.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }}"
    val response = sendUdpCommand(writeCommand, retroArchHost, retroArchPort)

    if (response != null && response.startsWith("WRITE_CORE_MEMORY")) {
        println("✅ Successfully gave Samus the grappling beam!")
        println("🎮 You should now have the grappling beam in your inventory")
    } else {
        println("❌ Error: Failed to write to memory")
        println("Response: $response")
    }

    // Also need to set the "equipped" bit for grappling beam
    // According to SRAM.md, this is at 0x3:6 (offset 0x3, bit 6)
    val equipAddress = 0x7E0003
    val equipData = readMemoryRange(equipAddress, 1, retroArchHost, retroArchPort)

    if (equipData != null && equipData.size == 1) {
        val currentEquip = equipData[0].toInt() and 0xFF
        val newEquip = currentEquip or 0x40  // Set bit 6

        val writeEquipCommand = "WRITE_CORE_MEMORY 0x${equipAddress.toString(16).uppercase()} ${newEquip.toString(16).padStart(2, '0')}"
        val equipResponse = sendUdpCommand(writeEquipCommand, retroArchHost, retroArchPort)

        if (equipResponse != null && equipResponse.startsWith("WRITE_CORE_MEMORY")) {
            println("✅ Successfully equipped the grappling beam!")
        } else {
            println("⚠️ Warning: Failed to equip the grappling beam")
        }
    }
}

/**
 * Send a UDP command to RetroArch and get the response
 */
fun sendUdpCommand(command: String, host: String, port: Int): String? {
    println("🔌 Sending command '$command' to $host:$port")

    var socket: DatagramSocket? = null

    try {
        // Create a new socket
        socket = DatagramSocket()
        socket.soTimeout = 1000 // 1 second timeout

        // Convert command to bytes
        val commandBytes = command.toByteArray()

        // Setup destination address
        val address = InetAddress.getByName(host)

        // Create and send packet
        val sendPacket = DatagramPacket(commandBytes, commandBytes.size, address, port)
        socket.send(sendPacket)

        // Prepare to receive response
        val buffer = ByteArray(1024)
        val receivePacket = DatagramPacket(buffer, buffer.size)

        // Receive response
        socket.receive(receivePacket)

        val response = String(receivePacket.data, 0, receivePacket.length).trim()
        println("🔌 Received ${receivePacket.length} bytes. Response: $response")
        return response
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        return null
    } finally {
        // Always close the socket when done
        socket?.close()
    }
}

/**
 * Read a range of memory from RetroArch
 */
fun readMemoryRange(startAddress: Int, size: Int, host: String, port: Int): ByteArray? {
    println("🔌 Reading memory range 0x${startAddress.toString(16).uppercase()} (size: $size)")

    val command = "READ_CORE_MEMORY 0x${startAddress.toString(16).uppercase()} $size"
    val response = sendUdpCommand(command, host, port)

    if (response == null) {
        println("❌ Failed to read memory range - no response received")
        return null
    }

    if (!response.startsWith("READ_CORE_MEMORY")) {
        println("❌ Invalid response format: $response")
        return null
    }

    return try {
        val parts = response.split(' ', limit = 3)
        if (parts.size < 3) {
            println("❌ Invalid response format (not enough parts): $response")
            return null
        }

        val hexData = parts[2].replace(" ", "")
        if (hexData.isEmpty()) {
            println("❌ Empty hex data in response")
            return null
        }

        val byteArray = hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        println("🔌 Successfully parsed ${byteArray.size} bytes of memory data")

        byteArray
    } catch (e: Exception) {
        println("❌ Failed to parse memory data: ${e.message}")
        null
    }
}
