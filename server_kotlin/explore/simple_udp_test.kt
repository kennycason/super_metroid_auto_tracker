import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
fun main() {
    println("🧪 Testing basic UDP socket creation...")
    
    // Test 1: Basic socket creation
    val fd = socket(AF_INET, SOCK_DGRAM, 0)
    println("Socket creation result: $fd")
    
    if (fd < 0) {
        println("❌ Socket creation failed")
        return
    }
    
    println("✅ Socket created successfully!")
    
    // Test 2: Try to send to RetroArch
    memScoped {
        val command = "VERSION"
        val commandBytes = command.encodeToByteArray()
        
        val addr = alloc<sockaddr_in>()
        addr.sin_family = AF_INET.convert()
        addr.sin_port = ((55355 and 0xFF) shl 8 or (55355 shr 8)).convert()
        addr.sin_addr.s_addr = 0x0100007F.convert() // 127.0.0.1
        
        println("🔌 Attempting to send to RetroArch...")
        val result = sendto(
            fd,
            commandBytes.refTo(0),
            commandBytes.size.convert(),
            0,
            addr.ptr.reinterpret(),
            sizeOf<sockaddr_in>().convert()
        )
        
        println("Send result: $result bytes")
        
        if (result > 0) {
            println("✅ Successfully sent UDP packet!")
            
            // Try to receive
            val buffer = ByteArray(1024)
            val received = recv(fd, buffer.refTo(0), buffer.size.convert(), 0)
            println("Receive result: $received bytes")
            
            if (received > 0) {
                val response = buffer.decodeToString(0, received.toInt())
                println("📨 Response: $response")
            }
        }
    }
    
    close(fd)
    println("🧪 Test complete")
} 