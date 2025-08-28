#!/usr/bin/env python3
import socket
import struct
import time

def read_memory_udp(address, size=2):
    """Read memory from RetroArch via UDP"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.settimeout(1.0)
    
    try:
        # Format: READ_CORE_MEMORY address size
        message = f"READ_CORE_MEMORY {address:08x} {size}".encode('ascii')
        sock.sendto(message, ('localhost', 55355))
        
        response, _ = sock.recvfrom(1024)
        if len(response) >= size:
            # Convert to little-endian integer
            if size == 2:
                return struct.unpack('<H', response[:2])[0]
            elif size == 1:
                return response[0]
        return None
    except Exception as e:
        print(f"Error reading memory: {e}")
        return None
    finally:
        sock.close()

def main():
    print("Mother Brain HP Monitor")
    print("======================")
    
    # Mother Brain HP address: 0x7e0fcc (from logs)
    mb_hp_addr = 0x7e0fcc
    
    while True:
        try:
            hp = read_memory_udp(mb_hp_addr, 2)
            if hp is not None:
                print(f"Mother Brain HP: {hp} (0x{hp:04x})")
            else:
                print("Could not read Mother Brain HP")
            
            time.sleep(0.5)
        except KeyboardInterrupt:
            print("\nStopping monitor...")
            break

if __name__ == "__main__":
    main()
