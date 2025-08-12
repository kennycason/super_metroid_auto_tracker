#!/usr/bin/env python3
"""
🎨 COMPREHENSIVE PALETTE TEST 🎨

Since our VBlank hook works (no ship corruption), but we're not seeing color changes,
let's create a test that writes bright colors to EVERY possible palette address
to find which ones are actually visible during gameplay.
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

class ComprehensivePaletteTest:
    """Test EVERY possible palette address to find visible ones"""
    
    def __init__(self):
        self.patcher = MinimalIPSPatcher()
    
    def allocate_space(self, size: int) -> int:
        """Allocate space for our comprehensive test code"""
        return 0x2F8000
    
    def create_comprehensive_palette_test(self, output_ips: str) -> bool:
        """Create a test that hits EVERY palette region with bright colors"""
        
        print("🎨 COMPREHENSIVE PALETTE TEST")
        print("=" * 50)
        print("🎯 Writing bright colors to ALL palette regions")
        print("⚠️  This should reveal which addresses are actually visible!")
        print()
        
        # MASSIVE palette test - hit everything!
        chaos_code = bytes([
            # Save registers
            0x48, 0xDA, 0x5A,                    # PHA, PHX, PHY
            
            # Set data bank to $7E (WRAM)
            0xA9, 0x7E, 0x48, 0xAB,              # LDA #$7E, PHA, PLB
            
            # Test 1: Basic palette region (0x7EC000-0x7EC200)
            0xA9, 0xFF,                          # LDA #$FF (bright white)
            0x8D, 0x00, 0xC0,                    # STA $C000
            0x8D, 0x01, 0xC0,                    # STA $C001  
            0x8D, 0x02, 0xC0,                    # STA $C002
            0x8D, 0x03, 0xC0,                    # STA $C003
            0x8D, 0x04, 0xC0,                    # STA $C004
            0x8D, 0x05, 0xC0,                    # STA $C005
            0x8D, 0x06, 0xC0,                    # STA $C006
            0x8D, 0x07, 0xC0,                    # STA $C007
            
            # Test 2: UDP script's exact addresses
            0x8D, 0x20, 0xC0,                    # STA $C020 (UDP script region)
            0x8D, 0x25, 0xC0,                    # STA $C025
            0x8D, 0x40, 0xC0,                    # STA $C040
            0x8D, 0x45, 0xC0,                    # STA $C045
            0x8D, 0x60, 0xC0,                    # STA $C060
            0x8D, 0x65, 0xC0,                    # STA $C065
            0x8D, 0x80, 0xC0,                    # STA $C080
            0x8D, 0x85, 0xC0,                    # STA $C085
            
            # Test 3: Samus suit colors (palette line C = 0x7EC180)
            0xA9, 0x1F,                          # LDA #$1F (bright red in SNES format)
            0x8D, 0x80, 0xC1,                    # STA $C180 (Color C0)
            0x8D, 0x82, 0xC1,                    # STA $C182 (Color C1)
            0x8D, 0x84, 0xC1,                    # STA $C184 (Color C2)
            0x8D, 0x86, 0xC1,                    # STA $C186 (Color C3)
            
            # Test 4: HUD colors (should be always visible)
            0xA9, 0xE0,                          # LDA #$E0 (bright blue in SNES format)  
            0x8D, 0x12, 0xC0,                    # STA $C012 (Color 09 - energy tanks)
            0x8D, 0x14, 0xC0,                    # STA $C014 (Color 0A - mini-map)
            0x8D, 0x16, 0xC0,                    # STA $C016 (Color 0B - HUD elements)
            0x8D, 0x1A, 0xC0,                    # STA $C01A (Color 0D - HUD text)
            0x8D, 0x1C, 0xC0,                    # STA $C01C (Color 0E - HUD white)
            
            # Test 5: Try CGRAM direct approach (PPU registers)
            0xA9, 0x00,                          # LDA #$00
            0x8D, 0x21, 0x21,                    # STA $2121 (CGRAM address)
            0xA9, 0xFF,                          # LDA #$FF  
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data - low byte)
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data - high byte)
            
            # Test 6: Alternative palette locations
            0xA9, 0x00,                          # LDA #$00 (black - to see contrast)
            0x8D, 0xA0, 0xC0,                    # STA $C0A0 (Extended palette)
            0x8D, 0xC0, 0xC0,                    # STA $C0C0
            0x8D, 0xE0, 0xC0,                    # STA $C0E0
            0x8D, 0x00, 0xC1,                    # STA $C100
            
            # Restore and return
            0xAB,                                # PLB (restore data bank)
            0x7A, 0xFA, 0x68,                    # PLY, PLX, PLA
            0x6B,                                # RTL
        ])
        
        # Put comprehensive test code in free space
        code_address = self.allocate_space(len(chaos_code))
        self.patcher.add_patch(code_address, chaos_code, "Comprehensive Palette Test Code")
        
        # Hook at our WORKING VBlank address (0x90B5)
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL to test code
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5  # Our working VBlank hook
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at Working VBlank 0x90B5")
        
        # Create IPS patch file
        print("\n📄 Creating comprehensive palette test IPS...")
        if not self.patcher.create_ips_file(output_ips):
            return False
        
        print("\n✅ COMPREHENSIVE PALETTE TEST COMPLETE!")
        print(f"📄 IPS Patch: {output_ips}")
        print("\n🎨 WHAT THIS TESTS:")
        print("   → Basic palette region (0x7EC000-0x7EC007)")
        print("   → UDP script addresses (0x7EC020, 0x7EC025, etc.)")
        print("   → Samus suit colors (0x7EC180-0x7EC186)")
        print("   → HUD colors (0x7EC012-0x7EC01C)")
        print("   → Direct CGRAM writes ($2121/$2122)")
        print("   → Extended palette regions")
        print("\n🔍 EXPECTED RESULTS:")
        print("   → At least SOME area should show bright colors")
        print("   → This will reveal which memory addresses work")
        print("   → Focus on areas that DO change for final patch")
        
        return True

def main():
    """Create comprehensive palette test"""
    print("🎨 Comprehensive Palette Address Tester")
    print("=" * 60)
    print("📄 Generating: super_metroid_comprehensive_palette_test.ips")
    print("🎯 Testing ALL possible palette addresses")
    print()
    
    # Create the palette tester
    tester = ComprehensivePaletteTest()
    
    try:
        # Generate the comprehensive test patch
        success = tester.create_comprehensive_palette_test("super_metroid_comprehensive_palette_test.ips")
        
        if success:
            print("\n🎉 SUCCESS! Comprehensive palette test ready!")
            print("\n🎨 TESTING STRATEGY:")
            print("   → This hits EVERY major palette region")
            print("   → Look for ANY color changes ANYWHERE")
            print("   → Note which specific areas change")
            print("   → We'll use working addresses for final patch")
            return 0
        else:
            print("\n❌ Failed to create comprehensive test")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())