import com.supermetroid.client.RetroArchUDPClient
import kotlinx.coroutines.runBlocking

/**
 * Simple script to test the RetroArchUDPClient directly
 */
fun main() = runBlocking {
    println("Testing RetroArchUDPClient...")

    val client = RetroArchUDPClient()

    // Test VERSION command
    println("\n=== Testing VERSION command ===")
    val versionResponse = client.sendCommand("VERSION")
    println("VERSION response: $versionResponse")

    // Test GET_STATUS command
    println("\n=== Testing GET_STATUS command ===")
    val statusResponse = client.sendCommand("GET_STATUS")
    println("GET_STATUS response: $statusResponse")

    // Test READ_CORE_MEMORY command
    println("\n=== Testing READ_CORE_MEMORY command ===")
    val memoryResponse = client.sendCommand("READ_CORE_MEMORY 0x7E09C2 2")
    println("READ_CORE_MEMORY response: $memoryResponse")

    // Test getRetroArchInfo
    println("\n=== Testing getRetroArchInfo ===")
    val info = client.getRetroArchInfo()
    println("Connected: ${info["connected"]}")
    println("RetroArch version: ${info["retroarch_version"]}")
    println("Game loaded: ${info["game_loaded"]}")
    println("Game info: ${info["game_info"]}")

    // Test readMemoryRange
    println("\n=== Testing readMemoryRange ===")
    val healthData = client.readMemoryRange(0x7E09C2, 2)
    println("Health data: ${healthData?.joinToString(" ") { it.toUByte().toString(16).padStart(2, '0') }}")

    // Summary
    println("\n=== Summary ===")
    println("VERSION: ${if (versionResponse != null) "Success" else "Failed"}")
    println("GET_STATUS: ${if (statusResponse != null) "Success" else "Failed"}")
    println("READ_CORE_MEMORY: ${if (memoryResponse != null) "Success" else "Failed"}")
    println("getRetroArchInfo: ${if (info["connected"] == true) "Success" else "Failed"}")
    println("readMemoryRange: ${if (healthData != null) "Success" else "Failed"}")
}
