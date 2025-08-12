#!/usr/bin/env python3
"""
🎯 MAIN GAME LOOP HOOK TEST 🎯

Based on ROM hacking documentation, let's try hooking into the actual
main game loop at address 0x82E4, which is commonly referenced in
Super Metroid ROM hacking guides as a main execution point.
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

class MainGameLoopTest:
    """Test hooking into the main game loop at 0x82E4"""
    
    def __init__(self):
        self.patcher = MinimalIPSPatcher()
    
    def allocate_space(self, size: int) -> int:
        """Allocate space for our test code"""
        return 0x2F8000
    
    def create_main_loop_test(self, output_ips: str) -> bool:
        """Test hooking into main game loop with palette AND graphics corruption"""
        
        print("🎯 MAIN GAME LOOP HOOK TEST")
        print("=" * 50)
        print("🎯 Testing main game loop at 0x82E4 (documented hook point)")
        print("⚠️  If this crashes, it means 0x82E4 is too critical")
        print("⚠️  If this works, we'll see BOTH palette AND graphics changes")
        print()
        
        # Comprehensive test with BOTH palette AND graphics corruption
        main_loop_test_code = bytes([
            # Minimal register saving (might be causing issues)
            0x48,                                # PHA (save A only)
            
            # Test 1: Graphics corruption (should be visible if executing)
            0xA9, 0x55,                          # LDA #$55 (pattern)
            0x8F, 0x00, 0x20, 0x7E,              # STA $7E2000 (sprite graphics)
            0x8F, 0x01, 0x20, 0x7E,              # STA $7E2001
            0x8F, 0x02, 0x20, 0x7E,              # STA $7E2002
            
            # Test 2: Try CGRAM direct writes (PPU registers)
            0xA9, 0x00,                          # LDA #$00
            0x8D, 0x21, 0x21,                    # STA $2121 (CGRAM address)
            0xA9, 0xFF,                          # LDA #$FF (bright white)
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data low)
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data high)
            
            # Test 3: Different CGRAM color
            0xA9, 0x01,                          # LDA #$01
            0x8D, 0x21, 0x21,                    # STA $2121 (CGRAM address 1)
            0xA9, 0x1F,                          # LDA #$1F (bright red)
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data low)
            0xA9, 0x00,                          # LDA #$00
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data high)
            
            # Test 4: More sprite corruption for visibility
            0xA9, 0xAA,                          # LDA #$AA (different pattern)
            0x8F, 0x10, 0x20, 0x7E,              # STA $7E2010
            0x8F, 0x20, 0x20, 0x7E,              # STA $7E2020
            0x8F, 0x30, 0x20, 0x7E,              # STA $7E2030
            
            # Restore and return
            0x68,                                # PLA (restore A)
            0x6B,                                # RTL
        ])
        
        # Put test code in free space
        code_address = self.allocate_space(len(main_loop_test_code))
        self.patcher.add_patch(code_address, main_loop_test_code, "Main Loop Test Code")
        
        # Hook at main game loop 0x82E4 (documented in ROM hacking guides)
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL to test code
            0xEA,  # NOP
        ])
        
        hook_address = 0x82E4  # Main game loop (ROM hacker documented address)
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at Main Game Loop 0x82E4")
        
        # Create IPS patch file
        print("\n📄 Creating main game loop test IPS...")
        if not self.patcher.create_ips_file(output_ips):
            return False
        
        print("\n✅ MAIN GAME LOOP TEST COMPLETE!")
        print(f"📄 IPS Patch: {output_ips}")
        print("\n🎯 EXPECTED RESULTS:")
        print("   🟢 IF GAME RUNS AND YOU SEE CHANGES:")
        print("      → Graphics corruption = code executes")
        print("      → Color changes = palette system works")
        print("      → We found our hook point!")
        print()
        print("   🔴 IF GAME CRASHES:")
        print("      → 0x82E4 is too critical for hooking")
        print("      → Need to find less critical execution point")
        print()
        print("   🟡 IF GAME RUNS BUT NO CHANGES:")
        print("      → 0x82E4 doesn't execute during gameplay")
        print("      → Need different hook location")
        print()
        print("🎯 This is our most promising test yet!")
        
        return True

def main():
    """Create main game loop test"""
    print("🎯 Main Game Loop Hook Tester")
    print("=" * 60)
    print("📄 Generating: super_metroid_main_loop_test.ips")
    print("🎯 Testing main game loop hook at 0x82E4")
    print()
    
    # Create the main loop tester
    tester = MainGameLoopTest()
    
    try:
        # Generate the main loop test
        success = tester.create_main_loop_test("super_metroid_main_loop_test.ips")
        
        if success:
            print("\n🎉 SUCCESS! Main game loop test ready!")
            print("\n🎯 THE DEFINITIVE TEST:")
            print("   → 0x82E4 is documented as main game loop")
            print("   → Should execute every frame during gameplay")
            print("   → Will show both graphics AND palette changes")
            print("   → This should finally work or crash definitively")
            return 0
        else:
            print("\n❌ Failed to create main loop test")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())