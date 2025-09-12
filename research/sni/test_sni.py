#!/usr/bin/env python3
"""
Simple SNI test script to detect devices and read SNES memory
Requires: pip install grpcio grpcio-tools
"""

import grpc
import sys
import os

# Add the generated protobuf files to path
sys.path.append('examples/golang/sni')

try:
    import sni_pb2
    import sni_pb2_grpc
except ImportError:
    print("❌ Error: Could not import SNI protobuf files")
    print("Run this first: cd examples/golang && ./generate.sh")
    sys.exit(1)

def test_sni():
    # Connect to SNI
    channel = grpc.insecure_channel('localhost:8191')
    
    # Create service stubs
    devices_stub = sni_pb2_grpc.DevicesStub(channel)
    memory_stub = sni_pb2_grpc.DeviceMemoryStub(channel)
    
    print("🔍 Testing SNI connection...")
    
    try:
        # Test 1: List devices
        print("\n📋 Listing devices...")
        response = devices_stub.ListDevices(sni_pb2.DevicesRequest())
        
        if not response.devices:
            print("❌ No devices found! Make sure your FX Pak Pro is connected.")
            return
            
        print(f"✅ Found {len(response.devices)} device(s):")
        for device in response.devices:
            print(f"   • {device.displayName} ({device.kind})")
            print(f"     URI: {device.uri}")
            print(f"     Capabilities: {list(device.capabilities)}")
        
        # Test 2: Read memory from first device
        first_device = response.devices[0]
        print(f"\n🧠 Testing memory read from: {first_device.displayName}")
        
        # Read 16 bytes from WRAM ($7E0010 in SNES address space)
        memory_request = sni_pb2.ReadMemoryRequest(
            requestAddress=0x7E0010,
            requestAddressSpace=sni_pb2.AddressSpace.SNES,
            requestMemoryMapping=sni_pb2.MemoryMapping.LoROM,
            size=16
        )
        
        read_response = memory_stub.SingleRead(
            sni_pb2.SingleReadMemoryRequest(
                uri=first_device.uri,
                request=memory_request
            )
        )
        
        data = read_response.response.data
        print(f"✅ Successfully read {len(data)} bytes:")
        print(f"   Address: ${read_response.response.requestAddress:06X}")
        print(f"   Data: {' '.join(f'{b:02X}' for b in data)}")
        
    except grpc.RpcError as e:
        print(f"❌ gRPC Error: {e.details()}")
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        channel.close()

if __name__ == "__main__":
    test_sni()
