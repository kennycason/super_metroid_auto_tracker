#!/usr/bin/env python3
"""
🎯 AGGRESSIVE 0x90B5 TEST 🎯

Since 0x90B5 works but is infrequent, let's make each execution do 
MASSIVE corruption that should be impossible to miss!
"""

import struct

class AggressiveIPSPatcher:
    """Aggressive corruption for infrequent hooks"""
    
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

class Aggressive90B5Test:
    """Make 0x90B5 do massive corruption per execution"""
    
    def __init__(self):
        self.patcher = AggressiveIPSPatcher()
    
    def create_massive_corruption_test(self, output_ips: str) -> bool:
        """Create test that corrupts huge amounts of memory per execution"""
        
        print("🎯 MASSIVE CORRUPTION TEST")
        print("=" * 50)
        print("💥 Each 0x90B5 execution corrupts 100+ memory locations")
        print("🎯 Should be IMPOSSIBLE to miss!")
        print("📍 Targeting sprites AND palettes aggressively")
        print()
        
        # Build massive corruption code
        corruption_code_parts = [
            # Save registers
            bytes([0x48, 0xDA, 0x5A, 0x8B]),  # PHA, PHX, PHY, PHB
            bytes([0xA9, 0x7E, 0x48, 0xAB]),  # LDA #$7E, PHA, PLB
        ]
        
        # Add sprite corruption (0x7E2000-0x7E20FF) - 256 bytes!
        sprite_corruption = []
        for i in range(0, 256, 4):  # Every 4th byte to avoid overwhelming
            sprite_corruption.extend([
                0xA9, 0xFF,  # LDA #$FF
                0x8F, i & 0xFF, 0x20, 0x7E,  # STA $7E20xx
            ])
        
        # Add palette corruption - hit EVERY palette address we know
        palette_addresses = [
            # WRAM palette addresses from UDP script
            0x7EC005, 0x7EC025, 0x7EC045, 0x7EC065, 0x7EC085,
            0x7EC000, 0x7EC020, 0x7EC040, 0x7EC060, 0x7EC080,
            0x7EC100, 0x7EC120, 0x7EC140, 0x7EC160, 0x7EC180,
            # HUD and UI palettes
            0x7EC200, 0x7EC220, 0x7EC240, 0x7EC260, 0x7EC280,
        ]
        
        palette_corruption = []
        for addr in palette_addresses:
            palette_corruption.extend([
                0xA9, 0xFF,  # LDA #$FF (bright white)
                0x8F, addr & 0xFF, (addr >> 8) & 0xFF, (addr >> 16) & 0xFF,  # STA long
            ])
        
        # Add more sprite areas for good measure
        more_sprite_corruption = []
        sprite_areas = [0x7E1000, 0x7E3000, 0x7E4000, 0x7E5000]
        for base in sprite_areas:
            for i in range(0, 32):  # First 32 bytes of each area
                more_sprite_corruption.extend([
                    0xA9, 0xAA,  # LDA #$AA (alternating pattern)
                    0x8F, i & 0xFF, (base >> 8) & 0xFF, (base >> 16) & 0xFF,  # STA long
                ])
        
        # Restore registers
        restore_code = bytes([0xAB, 0x7A, 0xFA, 0x68, 0x6B])  # PLB, PLY, PLX, PLA, RTL
        
        # Combine all parts
        massive_code = b''.join([
            b''.join(corruption_code_parts),
            bytes(sprite_corruption[:200]),  # Limit to avoid huge file
            bytes(palette_corruption),
            bytes(more_sprite_corruption[:100]),  # Limit sprite corruption
            restore_code
        ])
        
        print(f"📊 Generated {len(massive_code)} bytes of corruption code")
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, massive_code, "Massive Corruption Code")
        
        # Hook at safe 0x90B5
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_palette_focused_test(self, output_ips: str) -> bool:
        """Focus specifically on palette corruption for color changes"""
        
        print("🎯 PALETTE FOCUSED TEST")
        print("=" * 50)
        print("🌈 Focus ONLY on palette memory for color changes")
        print("📍 Hit every known palette address with bright colors")
        print()
        
        # Simple but comprehensive palette corruption
        palette_code = bytes([
            # Save registers
            0x48, 0xDA, 0x5A, 0x8B,  # PHA, PHX, PHY, PHB
            0xA9, 0x7E, 0x48, 0xAB,  # LDA #$7E, PHA, PLB
            
            # Bright white to primary palette locations
            0xA9, 0xFF,  # LDA #$FF
            0x8D, 0x05, 0xC0,  # STA $C005 (0x7EC005)
            0x8D, 0x25, 0xC0,  # STA $C025 (0x7EC025)
            0x8D, 0x45, 0xC0,  # STA $C045 (0x7EC045)
            0x8D, 0x65, 0xC0,  # STA $C065 (0x7EC065)
            0x8D, 0x85, 0xC0,  # STA $C085 (0x7EC085)
            
            # Bright colors to HUD area
            0x8D, 0x00, 0xC0,  # STA $C000 (0x7EC000)
            0x8D, 0x20, 0xC0,  # STA $C020 (0x7EC020)
            0x8D, 0x40, 0xC0,  # STA $C040 (0x7EC040)
            0x8D, 0x60, 0xC0,  # STA $C060 (0x7EC060)
            0x8D, 0x80, 0xC0,  # STA $C080 (0x7EC080)
            
            # Extended palette areas
            0x8F, 0x00, 0xC1, 0x7E,  # STA $7EC100
            0x8F, 0x20, 0xC1, 0x7E,  # STA $7EC120
            0x8F, 0x40, 0xC1, 0x7E,  # STA $7EC140
            0x8F, 0x60, 0xC1, 0x7E,  # STA $7EC160
            0x8F, 0x80, 0xC1, 0x7E,  # STA $7EC180
            
            # Samus suit palettes (the guide mentioned these)
            0x8F, 0x80, 0xC1, 0x7E,  # STA $7EC180 (Palette Line C)
            0x8F, 0x81, 0xC1, 0x7E,  # STA $7EC181
            0x8F, 0x82, 0xC1, 0x7E,  # STA $7EC182
            0x8F, 0x83, 0xC1, 0x7E,  # STA $7EC183
            
            # Also add a tiny bit of sprite corruption for execution proof
            0xA9, 0xAA,  # LDA #$AA
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000
            0x8F, 0x01, 0x20, 0x7E,  # STA $7E2001
            
            # Restore registers
            0xAB, 0x7A, 0xFA, 0x68,  # PLB, PLY, PLX, PLA
            0x6B,  # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, palette_code, "Palette Focused Corruption")
        
        # Hook at 0x90B5
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)

def main():
    """Generate aggressive 0x90B5 tests"""
    print("🎯 Aggressive 0x90B5 Corruption Tests")
    print("=" * 60)
    print("💥 Making each infrequent execution do MASSIVE corruption!")
    print()
    
    try:
        # Test 1: Massive corruption
        print("📄 Generating Test 1: Massive corruption...")
        test1 = Aggressive90B5Test()
        test1_success = test1.create_massive_corruption_test("super_metroid_massive_corruption.ips")
        
        # Test 2: Palette focused
        print("\n📄 Generating Test 2: Palette focused...")
        test2 = Aggressive90B5Test()
        test2_success = test2.create_palette_focused_test("super_metroid_palette_focused.ips")
        
        if all([test1_success, test2_success]):
            print("\n🎉 AGGRESSIVE TESTS GENERATED!")
            print("\n🔍 TEST ORDER:")
            print("   1️⃣ super_metroid_palette_focused.ips - Should show color changes!")
            print("   2️⃣ super_metroid_massive_corruption.ips - Should show HEAVY corruption!")
            print("\n🎯 EXPECTATIONS:")
            print("   🌈 Palette test: Should see bright white colors appear")
            print("   💥 Massive test: Should see heavy sprite corruption")
            print("   ✅ If either works: 0x90B5 is our solution!")
            return 0
        else:
            print("\n❌ Failed to generate tests")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())