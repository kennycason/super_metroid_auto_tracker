#!/usr/bin/env python3
import socket
import struct

def read_memory_udp(address, size=2):
    """Read memory from RetroArch via UDP"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.settimeout(2.0)
    
    try:
        message = f"READ_CORE_MEMORY {address:08x} {size}".encode('ascii')
        print(f"Sending: {message}")
        sock.sendto(message, ('localhost', 55355))
        response, _ = sock.recvfrom(1024)
        print(f"Raw response length: {len(response)}, data: {response[:10]}")
        if len(response) >= size:
            if size == 2:
                value = struct.unpack('<H', response[:2])[0]
                print(f"Parsed value: {value} (0x{value:04x})")
                return value
            elif size == 1:
                return response[0]
        return None
    except Exception as e:
        print(f"Error reading {address:08x}: {e}")
        return None
    finally:
        sock.close()

def main():
    print("Testing RetroArch UDP Memory Reading")
    print("====================================")
    
    # Test different addresses
    test_addresses = [
        (0x7e0fcc, "Mother Brain HP"),
        (0x7e079b, "Room ID"),
        (0x7e0998, "Game State"),
        (0x7ed821, "Event Flags"),
        (0x7e09c4, "Max Health")
    ]
    
    for addr, name in test_addresses:
        print(f"\nTesting {name} at 0x{addr:08x}:")
        value = read_memory_udp(addr, 2)
        if value is not None:
            print(f"{name}: {value} (0x{value:04x})")
        else:
            print(f"{name}: Failed to read")

if __name__ == "__main__":
    main()
