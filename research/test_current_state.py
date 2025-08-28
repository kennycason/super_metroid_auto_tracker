#!/usr/bin/env python3
import socket
import struct

def read_memory_udp(address, size=2):
    """Read memory from RetroArch via UDP"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.settimeout(1.0)
    
    try:
        message = f"READ_CORE_MEMORY {address:08x} {size}".encode('ascii')
        sock.sendto(message, ('localhost', 55355))
        response, _ = sock.recvfrom(1024)
        if len(response) >= size:
            if size == 2:
                return struct.unpack('<H', response[:2])[0]
            elif size == 1:
                return response[0]
        return None
    except Exception as e:
        print(f"Error reading {address:08x}: {e}")
        return None
    finally:
        sock.close()

def main():
    print("Current Game State Check")
    print("========================")
    
    # Memory addresses from the Kotlin app
    mb_hp = read_memory_udp(0x7e0fcc, 2)        # Mother Brain HP
    room_id = read_memory_udp(0x7e079b, 2)      # Room ID  
    game_state = read_memory_udp(0x7e0998, 2)   # Game State
    event_flags = read_memory_udp(0x7ed821, 2)  # Event Flags
    
    print(f"Mother Brain HP: {mb_hp} (0x{mb_hp:04x})")
    print(f"Room ID: {room_id} (0x{room_id:04x})")
    print(f"Game State: {game_state}")
    print(f"Event Flags: {event_flags} (0x{event_flags:04x})")
    
    # Check retroactive conditions
    print("\nRetroactive Logic Check:")
    mb_room = room_id == 0xDD58  # Mother Brain room from ASL
    normal_gameplay = game_state == 8
    fighting_mb2 = mb_hp == 17746
    zebes_escaping = (event_flags & 0x0040) != 0
    
    print(f"In MB Room: {mb_room} (room={room_id:04x}, expected=DD58)")
    print(f"Normal Gameplay: {normal_gameplay} (state={game_state}, expected=8)")
    print(f"Fighting MB2: {fighting_mb2} (HP={mb_hp}, expected=17746)")
    print(f"Zebes Escaping: {zebes_escaping} (eventFlags=0x{event_flags:04x}, bit6={(event_flags & 0x0040) != 0})")
    
    mb1_should_trigger = mb_room and normal_gameplay and (fighting_mb2 or zebes_escaping)
    print(f"\n=> MB1 Should Trigger: {mb1_should_trigger}")

if __name__ == "__main__":
    main()
