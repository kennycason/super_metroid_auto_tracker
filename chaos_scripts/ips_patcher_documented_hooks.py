#!/usr/bin/env python3
"""
🎯 DOCUMENTED HOOK POINTS TEST 🎯

Based on web search of Super Metroid ROM hacking documentation,
let's test actual documented hook points that execute during gameplay!
"""

import struct

class DocumentedHookPatcher:
    """Test documented hook points from ROM hacking guides"""
    
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

class DocumentedHookTest:
    """Test hook points documented in ROM hacking guides"""
    
    def __init__(self):
        self.patcher = DocumentedHookPatcher()
    
    def create_main_loop_hook_test(self, output_ips: str) -> bool:
        """Test 0x8290 - Main game loop (different from the crashing 0x82E4)"""
        
        print("🎯 MAIN GAME LOOP HOOK TEST")
        print("=" * 50)
        print("🎮 Hook: 0x8290 - Main game processing loop")
        print("📍 Should execute every frame during gameplay")
        print()
        
        # Simple but visible corruption for main loop
        main_loop_code = bytes([
            # Save registers (minimal)
            0x48,  # PHA
            
            # Simple sprite corruption for execution proof
            0xA9, 0xFF,  # LDA #$FF
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000 (sprite graphics)
            0xA9, 0x00,  # LDA #$00
            0x8F, 0x01, 0x20, 0x7E,  # STA $7E2001 (alternating pattern)
            0xA9, 0xFF,  # LDA #$FF
            0x8F, 0x02, 0x20, 0x7E,  # STA $7E2002
            
            # Simple palette corruption
            0xA9, 0xFF,  # LDA #$FF (bright white)
            0x8F, 0x05, 0xC0, 0x7E,  # STA $7EC005 (known palette address)
            0x8F, 0x25, 0xC0, 0x7E,  # STA $7EC025
            
            # Restore and return
            0x68,  # PLA
            0x6B,  # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, main_loop_code, "Main Game Loop Corruption")
        
        # Hook at 0x8290 (main game loop, not the crashing 0x82E4)
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x8290
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x8290 (Main Game Loop)")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_vblank_hook_test(self, output_ips: str) -> bool:
        """Test 0x808F - VBlank interrupt handler (should be very frequent)"""
        
        print("🎯 VBLANK INTERRUPT HOOK TEST")
        print("=" * 50)
        print("🎮 Hook: 0x808F - VBlank interrupt handler")
        print("📍 Should execute 60 times per second!")
        print()
        
        # VBlank corruption (should be VERY frequent)
        vblank_code = bytes([
            # Save registers (minimal for interrupt)
            0x48,  # PHA
            
            # Heavy sprite corruption (should be very visible due to frequency)
            0xA9, 0xAA,  # LDA #$AA (alternating pattern)
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000
            0x8F, 0x01, 0x20, 0x7E,  # STA $7E2001
            0x8F, 0x02, 0x20, 0x7E,  # STA $7E2002
            0x8F, 0x03, 0x20, 0x7E,  # STA $7E2003
            
            # Heavy palette corruption
            0xA9, 0xFF,  # LDA #$FF (bright white)
            0x8F, 0x05, 0xC0, 0x7E,  # STA $7EC005
            0x8F, 0x25, 0xC0, 0x7E,  # STA $7EC025
            0x8F, 0x45, 0xC0, 0x7E,  # STA $7EC045
            
            # Restore and return
            0x68,  # PLA
            0x6B,  # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, vblank_code, "VBlank Interrupt Corruption")
        
        # Hook at 0x808F (VBlank interrupt)
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x808F
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x808F (VBlank Interrupt)")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_frame_counter_hook_test(self, output_ips: str) -> bool:
        """Test hooking into frame counter update (should execute every frame)"""
        
        print("🎯 FRAME COUNTER HOOK TEST")
        print("=" * 50)
        print("🎮 Hook: Frame counter area (around 0x05B4 updates)")
        print("📍 Should execute every single frame")
        print()
        
        # Let's try hooking around where frame counter gets updated
        # This should be very frequent
        frame_code = bytes([
            # Save registers
            0x48,  # PHA
            
            # Read frame counter and use it for chaos
            0xAD, 0xB4, 0x05,  # LDA $05B4 (frame counter)
            0x29, 0x07,        # AND #$07 (every 8 frames)
            0xF0, 0x0C,        # BEQ skip_corruption (branch if zero)
            
            # Sprite corruption when frame counter is not divisible by 8
            0xA9, 0xFF,  # LDA #$FF
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000
            0xA9, 0x00,  # LDA #$00
            0x8F, 0x01, 0x20, 0x7E,  # STA $7E2001
            
            # Palette corruption
            0xA9, 0xFF,  # LDA #$FF
            0x8F, 0x05, 0xC0, 0x7E,  # STA $7EC005
            
            # skip_corruption:
            0x68,  # PLA
            0x6B,  # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, frame_code, "Frame Counter Chaos")
        
        # Try hooking at 0x809D (documented game processing area)
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x809D
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x809D (Game Processing)")
        
        return self.patcher.create_ips_file(output_ips)

def main():
    """Generate documented hook point tests"""
    print("🎯 Documented Hook Points Test")
    print("=" * 60)
    print("📚 Testing REAL hook points from ROM hacking documentation")
    print()
    
    try:
        # Test 1: Main game loop (different address than the crashing one)
        print("📄 Generating Test 1: Main game loop...")
        test1 = DocumentedHookTest()
        test1_success = test1.create_main_loop_hook_test("super_metroid_main_loop_8290.ips")
        
        # Test 2: VBlank interrupt handler
        print("\n📄 Generating Test 2: VBlank interrupt...")
        test2 = DocumentedHookTest()
        test2_success = test2.create_vblank_hook_test("super_metroid_vblank_808f.ips")
        
        # Test 3: Frame counter area
        print("\n📄 Generating Test 3: Frame counter area...")
        test3 = DocumentedHookTest()
        test3_success = test3.create_frame_counter_hook_test("super_metroid_frame_counter.ips")
        
        if all([test1_success, test2_success, test3_success]):
            print("\n🎉 DOCUMENTED HOOK TESTS GENERATED!")
            print("\n🔍 TEST PRIORITY ORDER:")
            print("   1️⃣ super_metroid_vblank_808f.ips - VBlank (60fps execution)")
            print("   2️⃣ super_metroid_main_loop_8290.ips - Main loop (every frame)")
            print("   3️⃣ super_metroid_frame_counter.ips - Frame processing")
            print("\n🎯 EXPECTATIONS:")
            print("   🌈 VBlank test: Should show CONSTANT heavy corruption")
            print("   🎮 Main loop test: Should show frequent sprite/palette changes")
            print("   ⏱️ Frame counter test: Should show rhythmic corruption pattern")
            print("\n💡 THESE ARE REAL EXECUTION POINTS!")
            print("   → If any work, we can build the final chaos patch!")
            return 0
        else:
            print("\n❌ Failed to generate tests")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())