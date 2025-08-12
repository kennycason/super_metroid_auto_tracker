# Super Metroid HUD Kotlin Server UDP Client Fix

## Issue Description

The Super Metroid HUD Kotlin server was not correctly communicating with RetroArch via UDP. The server was returning cached or mock data instead of the actual game state. This was evident from the `/api/status` endpoint returning data that didn't match the actual game state:

- The game info showed "Super Metroid Z-Factor" instead of "Super Metroid (Japan, USA)"
- Health, missiles, and other stats didn't match the actual game state
- The area was shown as Norfair (area_id = 2) instead of Tourian (area_id = 5)
- Not all items and bosses were correctly detected

## Root Causes

After investigating the issue, we identified the following root causes:

1. **Socket Timeout**: The RetroArchUDPClient didn't set a timeout on the socket, which could cause the `recvfrom` call to block indefinitely if no response was received.

2. **Socket Reuse**: The RetroArchUDPClient tried to reuse the socket for multiple commands, which could lead to issues if the socket state became invalid.

3. **Error Handling**: The RetroArchUDPClient had complex error handling that might have been hiding issues.

4. **Clearing Pending Data**: The RetroArchUDPClient tried to clear any pending data before sending a command, which could potentially cause issues if it failed.

5. **Mock Client Usage**: The BackgroundPoller was using a MockRetroArchUDPClient instead of the real RetroArchUDPClient, which always returns the same static data regardless of the actual game state.

## Changes Made

### 1. RetroArchUDPClient.kt

We completely rewrote the RetroArchUDPClient implementation to be more similar to the Python implementation that works correctly:

- Created a new socket for each command (like the Python implementation)
- Set a timeout on the socket (like Python's sock.settimeout(1.0))
- Simplified the error handling
- Removed the clearing of pending data
- Closed the socket after each command (like the Python implementation)

### 2. BackgroundPoller.kt

We modified the BackgroundPoller to use the real RetroArchUDPClient instead of the MockRetroArchUDPClient:

```kotlin
// Use real UDP client for production
private val udpClient = RetroArchUDPClient()

// For testing, uncomment this and comment out the real client above
// private val udpClient = MockRetroArchUDPClient().apply {
//     setMockMode(MockRetroArchUDPClient.MockMode.STATIC)
//     setStaticState(1) // 0=Crateria, 1=Norfair, 2=Lower Norfair
// }
```

## Testing

We tested the changes by:

1. Creating a Python script (`test_retroarch_udp.py`) to test UDP communication with RetroArch directly
2. Verifying that RetroArch is running and responding to UDP commands
3. Restarting the server with the fixed RetroArchUDPClient
4. Testing the `/api/status` endpoint to verify that it returns the correct game state

The tests confirmed that the server now correctly communicates with RetroArch and returns the actual game state.

## Results

The `/api/status` endpoint now returns the correct game state:

```json
{
    "connected": true,
    "game_loaded": true,
    "retroarch_version": "1.21.0",
    "game_info": "GET_STATUS PLAYING super_nes,Super Metroid (Japan, USA) (En,Ja),crc32=d63ed5f8",
    "stats": {
        "health": 1099,
        "max_health": 1099,
        "missiles": 135,
        "max_missiles": 135,
        "supers": 22,
        "max_supers": 30,
        "power_bombs": 30,
        "max_power_bombs": 30,
        "reserve_energy": 300,
        "room_id": 56867,
        "area_id": 5,
        "area_name": "Tourian",
        "game_state": 15,
        "player_x": 111,
        "player_y": 155,
        "items": {
            "morph": true,
            "bombs": true,
            "varia": true,
            "gravity": true,
            "hijump": true,
            "speed": true,
            "space": true,
            "screw": true,
            "spring": true,
            "grapple": true,
            "xray": true
        },
        "beams": {
            "charge": true,
            "ice": true,
            "wave": true,
            "spazer": true,
            "plasma": true,
            "hyper": false
        },
        "bosses": {
            "bomb_torizo": true,
            "kraid": true,
            "spore_spawn": true,
            "mother_brain": false,
            "crocomire": false,
            "phantoon": true,
            "botwoon": true,
            "draygon": true,
            "ridley": false,
            "golden_torizo": false,
            "mother_brain_1": false,
            "mother_brain_2": false,
            "samus_ship": false
        }
    },
    "last_update": 1753904503224,
    "poll_count": 17
}
```

## Usage Instructions

### Starting the Server

To start the server, run:

```bash
bash manage_server.sh start
```

The server will start on port 8082 by default. You can access the API at:

- API Status: http://localhost:8081/api/status
- API Stats: http://localhost:8081/api/stats
- Tracker UI: http://localhost:8081/

### Stopping the Server

To stop the server, run:

```bash
bash manage_server.sh stop
```

### Checking Server Status

To check the server status, run:

```bash
bash manage_server.sh status
```

### Viewing Server Logs

To view the server logs, run:

```bash
bash manage_server.sh logs
```

### Troubleshooting

If you encounter issues with the server:

1. Make sure RetroArch is running and has a Super Metroid ROM loaded
2. Check the server logs for errors
3. Try restarting the server
4. If the server won't start due to port conflicts, use the `explore/port_check.sh` script to diagnose and fix the issue

## Conclusion

The Super Metroid HUD Kotlin server now correctly communicates with RetroArch and returns the actual game state. The changes we made fixed the issues with the UDP client implementation and ensured that the server works reliably.

If you encounter any issues or have questions, please refer to the documentation or contact the development team.
