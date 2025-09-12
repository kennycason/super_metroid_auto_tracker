# SNI Integration Guide for Super Metroid Tracker

## Overview
SNI (Super Nintendo Interface) provides a unified gRPC API for accessing SNES devices, including FX Pak Pro hardware and emulators. This guide helps migrate from BSNES+RetroArch NWA to SNI for your Kotlin Super Metroid tracker.

## Key Advantages of SNI over NWA
- **Unified API**: Same interface for hardware (FX Pak Pro) and emulators
- **Better Protocol**: gRPC instead of UDP packets
- **Concurrent Access**: Multiple applications can connect simultaneously
- **Stateless**: No connection management required
- **Cross-Platform**: Works on Windows, macOS, and Linux

## Architecture

### SNI Service (localhost:8191)
```
┌─────────────────┐    gRPC     ┌─────────────────┐
│ Your Kotlin App │ ◄────────► │ SNI Service     │
└─────────────────┘             └─────────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    ▼                    ▼                    ▼
            ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
            │ FX Pak Pro   │    │ RetroArch    │    │ Other        │
            │ (Hardware)   │    │ (Emulator)   │    │ Emulators    │
            └──────────────┘    └──────────────┘    └──────────────┘
```

## Connection Information

### Default Ports
- **gRPC API**: `localhost:8191` (your main integration point)
- **gRPC-Web**: `localhost:8190` (for web browsers)
- **USB2SNES Compat**: `localhost:23074` (legacy WebSocket)

### Device Detection
Always call `ListDevices()` to discover available SNES devices:
```kotlin
// Pseudo-code for device discovery
val devices = sniClient.listDevices()
val preferredDevice = devices.find { it.kind == "fxpakpro" } 
    ?: devices.firstOrNull()
```

## Memory Access

### Address Spaces
SNI supports 3 address spaces (choose based on your needs):

1. **SNES A-bus** (`SNESABUS`) - Most familiar for SNES developers
   - Example: `$7E0010` for WRAM
   - Requires memory mapping mode (LoROM/HiROM)

2. **FX Pak Pro** (`FXPAKPRO`) - Direct hardware mapping
   - Example: `$F50010` for WRAM
   - No mapping mode needed
   - Best for hardware compatibility

3. **Raw** (`RAW`) - Direct device addresses
   - No translation performed
   - Use when you know exact device layout

### Super Metroid Memory Locations (SNES A-bus)
```kotlin
// Key memory addresses for Super Metroid tracking
val GAME_STATE = 0x7E0998        // Game state
val SAMUS_X = 0x7E0AF6          // Samus X position  
val SAMUS_Y = 0x7E0AFA          // Samus Y position
val CURRENT_ENERGY = 0x7E09C2    // Current energy
val MAX_ENERGY = 0x7E09C4        // Max energy
val MISSILES = 0x7E09C6          // Current missiles
val SUPER_MISSILES = 0x7E09CA    // Current super missiles
val POWER_BOMBS = 0x7E09CE       // Current power bombs
val ITEMS_COLLECTED = 0x7E09A4   // Items collected bitmask
val CURRENT_ROOM = 0x7E079B      // Current room ID
```

### Memory Reading Example
```kotlin
// Single memory read
val request = SingleReadMemoryRequest.newBuilder()
    .setUri(deviceUri)
    .setRequest(ReadMemoryRequest.newBuilder()
        .setRequestAddress(0x7E0998)  // Game state
        .setRequestAddressSpace(AddressSpace.SNESABUS)
        .setRequestMemoryMapping(MemoryMapping.LOROM)
        .setSize(2)  // Read 2 bytes
        .build())
    .build()

val response = memoryClient.singleRead(request)
val gameState = response.response.data.toByteArray()
```

### Batch Reading (Recommended)
```kotlin
// Read multiple locations efficiently
val requests = listOf(
    ReadMemoryRequest.newBuilder()
        .setRequestAddress(0x7E0998).setSize(2)  // Game state
        .setRequestAddressSpace(AddressSpace.SNESABUS)
        .setRequestMemoryMapping(MemoryMapping.LOROM)
        .build(),
    ReadMemoryRequest.newBuilder()
        .setRequestAddress(0x7E09C2).setSize(2)  // Energy
        .setRequestAddressSpace(AddressSpace.SNESABUS)
        .setRequestMemoryMapping(MemoryMapping.LOROM)
        .build()
)

val multiRequest = MultiReadMemoryRequest.newBuilder()
    .setUri(deviceUri)
    .addAllRequests(requests)
    .build()

val response = memoryClient.multiRead(multiRequest)
```

## Device Capabilities

### FX Pak Pro (Hardware)
- ✅ ReadMemory, WriteMemory
- ✅ ResetSystem, ResetToMenu  
- ✅ ExecuteASM, FetchFields
- ✅ File operations (ReadDirectory, PutFile, GetFile, etc.)
- ⚠️ WRAM reading limitations with certain enhancement chips

### RetroArch
- ✅ ReadMemory, WriteMemory
- ✅ PauseUnpauseEmulation, ResetSystem
- ⚠️ Address space detection may vary by core
- ⚠️ 16ms delay (processes during vsync)

### Lua Bridge (Snes9x-rr, BizHawk)
- ✅ ReadMemory, WriteMemory
- ✅ PauseToggleEmulation

## Performance Considerations

### Polling Strategy
```kotlin
// Recommended polling for real-time tracking
class SuperMetroidTracker {
    private val pollInterval = 100.milliseconds  // 10 FPS
    
    fun startTracking() {
        timer.scheduleAtFixedRate(pollInterval) {
            readGameState()
        }
    }
    
    private fun readGameState() {
        // Batch read all needed memory locations
        val gameData = batchReadMemory(TRACKING_ADDRESSES)
        updateTracker(gameData)
    }
}
```

### Connection Management
```kotlin
// SNI is stateless - no connection management needed
class SNIClient {
    private val channel = ManagedChannelBuilder
        .forAddress("localhost", 8191)
        .usePlaintext()
        .build()
    
    private val devicesClient = DevicesGrpc.newBlockingStub(channel)
    private val memoryClient = DeviceMemoryGrpc.newBlockingStub(channel)
    
    // Always get fresh device list
    fun getCurrentDevice(): Device? {
        return devicesClient.listDevices(DevicesRequest.getDefaultInstance())
            .devicesList
            .firstOrNull { it.capabilitiesList.contains(DeviceCapability.ReadMemory) }
    }
}
```

## Error Handling

### Common Issues
1. **No devices found**: Ensure SNES device is connected and recognized
2. **Memory read fails**: Check address space and memory mapping
3. **Connection refused**: Ensure SNI service is running on port 8191

```kotlin
fun safeMemoryRead(address: Int, size: Int): ByteArray? {
    return try {
        val device = getCurrentDevice() 
            ?: throw Exception("No SNES device available")
            
        val response = memoryClient.singleRead(
            buildReadRequest(device.uri, address, size)
        )
        response.response.data.toByteArray()
    } catch (e: Exception) {
        logger.warn("Memory read failed for address 0x${address.toString(16)}: ${e.message}")
        null
    }
}
```

## Setup Instructions

### 1. Start SNI Service
```bash
# Download/build SNI executable
./sni  # Runs on localhost:8191
```

### 2. Generate Kotlin gRPC Client
```bash
# Use protoc to generate Kotlin gRPC client from sni.proto
protoc --kotlin_out=src/main/kotlin \
       --grpc-kotlin_out=src/main/kotlin \
       --proto_path=protos/sni \
       sni.proto
```

### 3. Add Dependencies
```kotlin
// build.gradle.kts
dependencies {
    implementation("io.grpc:grpc-kotlin-stub:1.4.0")
    implementation("io.grpc:grpc-protobuf:1.58.0")
    implementation("io.grpc:grpc-netty:1.58.0")
}
```

## Environment Variables

Useful for debugging and customization:

```bash
export SNI_DEBUG=1                    # Enable debug logging
export SNI_GRPC_LISTEN_PORT=8191     # Change gRPC port
export SNI_RETROARCH_HOSTS=localhost:55355  # RetroArch detection
```

## Migration from NWA

### Key Differences
| Aspect | NWA (Old) | SNI (New) |
|--------|-----------|-----------|
| Protocol | UDP packets | gRPC |
| Connection | Manual connection management | Stateless |
| Address Format | Device-specific | Unified address spaces |
| Device Support | RetroArch only | FX Pak Pro + Emulators |
| Concurrency | Single client | Multiple clients |

### Migration Checklist
- [ ] Replace NWA UDP client with gRPC client
- [ ] Update memory addresses to use SNES A-bus space
- [ ] Remove connection management code
- [ ] Add device discovery logic
- [ ] Test with both hardware and emulator
- [ ] Update error handling for gRPC exceptions

## Testing
Use the included grpcui tool for interactive testing:
```bash
~/go/bin/grpcui -plaintext -port 8192 localhost:8191
# Open http://localhost:8192 in browser
```

This provides a web interface to test all SNI API calls before implementing in Kotlin.
