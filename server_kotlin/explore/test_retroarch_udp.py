#!/usr/bin/env python3
"""
Simple script to test UDP communication with RetroArch
"""

import socket
import time

def test_retroarch_udp(host="localhost", port=55355, command="VERSION"):
    """Test UDP communication with RetroArch"""
    print(f"Testing UDP communication with RetroArch at {host}:{port}")
    print(f"Command: {command}")

    try:
        # Create UDP socket
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.settimeout(1.0)  # 1 second timeout

        # Send command
        print(f"Sending command: {command}")
        sock.sendto(command.encode(), (host, port))

        # Receive response
        print("Waiting for response...")
        data, addr = sock.recvfrom(1024)
        response = data.decode().strip()

        print(f"Response from {addr}: {response}")
        return response
    except socket.timeout:
        print("ERROR: Socket timeout - no response received")
        return None
    except Exception as e:
        print(f"ERROR: {e}")
        return None
    finally:
        sock.close()

def main():
    """Main function"""
    # Test VERSION command
    print("=== Testing VERSION command ===")
    version_response = test_retroarch_udp(command="VERSION")

    # Test GET_STATUS command
    print("\n=== Testing GET_STATUS command ===")
    status_response = test_retroarch_udp(command="GET_STATUS")

    # Test READ_CORE_MEMORY command (read 2 bytes from 0x7E09C2 - health in Super Metroid)
    print("\n=== Testing READ_CORE_MEMORY command ===")
    memory_response = test_retroarch_udp(command="READ_CORE_MEMORY 0x7E09C2 2")

    # Summary
    print("\n=== Summary ===")
    print(f"VERSION: {'Success' if version_response else 'Failed'}")
    print(f"GET_STATUS: {'Success' if status_response else 'Failed'}")
    print(f"READ_CORE_MEMORY: {'Success' if memory_response else 'Failed'}")

if __name__ == "__main__":
    main()
