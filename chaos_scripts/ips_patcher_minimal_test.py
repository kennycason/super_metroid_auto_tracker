#!/usr/bin/env python3
"""
🔍 ULTRA-MINIMAL TEST 🔍

The fact that even NMI vector redirection crashes suggests our assembly code itself has issues.
Let's create the most minimal possible test - just hook and return, nothing else.
"""

import struct

class MinimalIPSPatcher:
    """Minimal IPS patcher for testing"""
    
    def __init__(self):
        self.patches = []
    
    def add_patch(self, address: int, data: bytes, description: str = ""):
        """Add a patch to the list"""
        self.patches.append({
            'address': address,
            'data': data,
            'description': description
        })
        print(f"📝 Added patch: 0x{address:06X} ({len(data)} bytes) - {description}")
    
    def create_ips_file(self, output_path: str) -> bool:
        """Create IPS patch file"""
        try:
            with open(output_path, 'wb') as f:
                # IPS header
                f.write(b'PATCH')
                
                # Write all patches
                for patch in self.patches:
                    address = patch['address']
                    data = patch['data']
                    
                    # Address (3 bytes, big-endian)
                    f.write(struct.pack('>I', address)[1:])
                    
                    # Size (2 bytes, big-endian)
                    f.write(struct.pack('>H', len(data)))
                    
                    # Data
                    f.write(data)
                
                # EOF marker
                f.write(b'EOF')
            
            print(f"✅ Created IPS patch: {output_path}")
            print(f"📊 Total patches: {len(self.patches)}")
            return True
            
        except Exception as e:
            print(f"❌ Error creating IPS: {e}")
            return False

class UltraMinimalTest:
    """Test with absolute minimum code - just return"""
    
    def __init__(self):
        self.patcher = MinimalIPSPatcher()
    
    def create_ultra_minimal_test(self, output_ips: str) -> bool:
        """Create ultra-minimal test with different return methods"""
        
        print("🔍 ULTRA-MINIMAL ASSEMBLY TEST")
        print("=" * 50)
        print("🎯 Testing if our basic assembly approach works at all")
        print("⚠️  This does NOTHING except return - no memory writes!")
        print()
        
        # Test 1: Just RTL (what we've been using)
        minimal_code_rtl = bytes([
            0x6B,  # RTL (Return from Long call)
        ])
        
        # Test 2: Just RTS (standard return from subroutine)  
        minimal_code_rts = bytes([
            0x60,  # RTS (Return from Subroutine)
        ])
        
        # Test 3: NOP then RTL
        minimal_code_nop_rtl = bytes([
            0xEA,  # NOP (No Operation)
            0x6B,  # RTL
        ])
        
        # Use the safest hook we found (0x90B5) with RTL approach
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, minimal_code_rtl, "Ultra-Minimal RTL Code")
        
        # Hook at 0x90B5 (we know this doesn't crash)
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Minimal Hook at 0x90B5")
        
        # Create IPS patch file
        print("\n📄 Creating ultra-minimal test IPS...")
        if not self.patcher.create_ips_file(output_ips):
            return False
        
        print("\n✅ ULTRA-MINIMAL TEST COMPLETE!")
        print(f"📄 IPS Patch: {output_ips}")
        print("\n🔍 WHAT THIS TESTS:")
        print("   → JSL instruction works correctly")
        print("   → RTL instruction works correctly") 
        print("   → Our bank addressing is right")
        print("   → Basic assembly syntax is correct")
        print("\n🎯 EXPECTED RESULTS:")
        print("   ✅ Game should load normally (no black screen)")
        print("   ✅ Should run exactly like unpatched game")
        print("   ✅ If this works, our assembly is fine")
        print("   ❌ If this crashes, our JSL/RTL approach is wrong")
        
        return True

def main():
    """Create ultra-minimal test"""
    print("🔍 Ultra-Minimal Assembly Tester")
    print("=" * 60)
    print("📄 Generating: super_metroid_ultra_minimal_test.ips")
    print("🎯 Testing if JSL/RTL assembly approach works at all")
    print()
    
    # Create the minimal tester
    tester = UltraMinimalTest()
    
    try:
        # Generate the minimal test
        success = tester.create_ultra_minimal_test("super_metroid_ultra_minimal_test.ips")
        
        if success:
            print("\n🎉 SUCCESS! Ultra-minimal test ready!")
            print("\n🔍 THE ROOT CAUSE TEST:")
            print("   → This tests our fundamental JSL/RTL approach")
            print("   → If this crashes, our assembly method is wrong")
            print("   → If this works, the issue is in our memory writes")
            print("   → This will isolate the exact problem!")
            return 0
        else:
            print("\n❌ Failed to create minimal test")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())