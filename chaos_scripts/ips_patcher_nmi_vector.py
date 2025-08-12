#!/usr/bin/env python3
"""
🎯 NMI VECTOR REDIRECTION TEST 🎯

This is the SAFEST and most reliable approach used by ROM hackers.
Instead of hooking into existing code, we redirect the NMI (Non-Maskable Interrupt)
vector at 0x7FEA to point to our custom handler.

NMI is triggered during VBlank - the perfect time for palette updates!
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

class NMIVectorTest:
    """Test using NMI vector redirection - the ROM hacker's gold standard"""
    
    def __init__(self):
        self.patcher = MinimalIPSPatcher()
    
    def allocate_space(self, size: int, offset: int = 0) -> int:
        """Allocate space for our code with optional offset"""
        return 0x2F8000 + offset
    
    def create_nmi_vector_test(self, output_ips: str) -> bool:
        """Create NMI vector redirection test - the ROM hacker way"""
        
        print("🎯 NMI VECTOR REDIRECTION TEST")
        print("=" * 50)
        print("🎯 Using NMI vector redirection at 0x7FEA (ROM hacker standard)")
        print("⚡ NMI triggers during VBlank - perfect for palette updates!")
        print("✅ This should be safe and execute regularly!")
        print()
        
        # Step 1: Create our chaos routine
        chaos_code = bytes([
            # Save registers (full save for safety)
            0x48, 0xDA, 0x5A, 0x8B,              # PHA, PHX, PHY, PHB
            
            # Set data bank to $7E for WRAM access
            0xA9, 0x7E, 0x48, 0xAB,              # LDA #$7E, PHA, PLB
            
            # Frame counter check (every 8 frames for smooth effect)
            0xAD, 0xB4, 0x05,                    # LDA $05B4 (frame counter)
            0x29, 0x07,                          # AND #$07 (every 8 frames)
            0xF0, 0x03,                          # BEQ skip_chaos
            0x4C, 0x20, 0x00,                    # JMP cleanup (relative jump to cleanup)
            
            # Multiple palette corruption methods for maximum visibility
            
            # Method 1: Direct CGRAM writes (PPU registers)
            0xA9, 0x00,                          # LDA #$00
            0x8D, 0x21, 0x21,                    # STA $2121 (CGRAM address 0)
            0xAD, 0xB4, 0x05,                    # LDA $05B4 (use frame counter for variation)
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data low)
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data high)
            
            # Method 2: Different CGRAM color
            0xA9, 0x01,                          # LDA #$01
            0x8D, 0x21, 0x21,                    # STA $2121 (CGRAM address 1)
            0xA9, 0xFF,                          # LDA #$FF (bright white)
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data low)
            0xA9, 0x1F,                          # LDA #$1F (bright value)
            0x8D, 0x22, 0x21,                    # STA $2122 (CGRAM data high)
            
            # Method 3: WRAM palette corruption (working UDP script approach)
            0xAD, 0xB4, 0x05,                    # LDA $05B4 (frame counter)
            0x18, 0x69, 0x33,                    # CLC, ADC #$33
            0x8D, 0x05, 0xC0,                    # STA $C005 (UDP script address)
            0x8D, 0x25, 0xC0,                    # STA $C025 
            0x8D, 0x65, 0xC0,                    # STA $C065
            
            # Method 4: Sprite graphics corruption (execution proof)
            0xA9, 0xAA,                          # LDA #$AA
            0x8F, 0x00, 0x20, 0x7E,              # STA $7E2000 (sprite graphics)
            0x8F, 0x10, 0x20, 0x7E,              # STA $7E2010
            
            # cleanup:
            0xAB, 0x7A, 0xFA, 0x68,              # PLB, PLY, PLX, PLA
            0x6B,                                # RTL
        ])
        
        # Step 2: Create custom NMI handler
        # This handler calls our chaos code then jumps to original NMI
        nmi_handler = bytes([
            # Call our chaos routine
            0x22, 0x00, 0x80, 0x2F,              # JSL $2F8000 (our chaos code)
            # Jump to original NMI handler (Super Metroid's default NMI is at 0x808F)
            0x5C, 0x8F, 0x80, 0x80,              # JMP $808F (original NMI handler)
        ])
        
        # Step 3: Place code in free ROM space
        chaos_address = self.allocate_space(len(chaos_code))
        nmi_handler_address = self.allocate_space(len(nmi_handler), len(chaos_code))
        
        # Update the JSL address in nmi_handler to point to actual chaos_address
        nmi_handler = bytes([
            0x22, chaos_address & 0xFF, (chaos_address >> 8) & 0xFF, (chaos_address >> 16) & 0xFF,
            0x5C, 0x8F, 0x80, 0x80,  # JMP $808F
        ])
        
        self.patcher.add_patch(chaos_address, chaos_code, "Chaos Palette Code")
        self.patcher.add_patch(nmi_handler_address, nmi_handler, "Custom NMI Handler")
        
        # Step 4: Redirect NMI vector to our custom handler
        nmi_vector = bytes([
            nmi_handler_address & 0xFF,          # Low byte of our NMI handler address
            (nmi_handler_address >> 8) & 0xFF,   # High byte of our NMI handler address  
        ])
        
        nmi_vector_address = 0x7FEA  # NMI vector location in SNES ROM
        self.patcher.add_patch(nmi_vector_address, nmi_vector, "NMI Vector Redirection")
        
        # Create IPS patch file
        print("\n📄 Creating NMI vector redirection test IPS...")
        if not self.patcher.create_ips_file(output_ips):
            return False
        
        print("\n✅ NMI VECTOR REDIRECTION TEST COMPLETE!")
        print(f"📄 IPS Patch: {output_ips}")
        print("\n⚡ HOW THIS WORKS:")
        print("   1. NMI vector at 0x7FEA now points to our custom handler")
        print("   2. During VBlank, NMI triggers our custom handler")
        print("   3. Custom handler calls our chaos code (JSL)")
        print("   4. Then jumps to original NMI handler (JMP)")
        print("   5. Game continues normally!")
        print("\n🎯 EXPECTED RESULTS:")
        print("   ✅ Game should run normally (no crashes)")
        print("   ✅ Should see graphics corruption (execution proof)")  
        print("   ✅ Should see color changes (palette effects)")
        print("   ✅ Effects should be smooth (VBlank timing)")
        print("\n🌊 This is the ROM hacker's gold standard approach!")
        
        return True

def main():
    """Create NMI vector redirection test"""
    print("⚡ NMI Vector Redirection Tester")
    print("=" * 60)
    print("📄 Generating: super_metroid_nmi_vector_test.ips")
    print("🎯 Using ROM hacker's gold standard: NMI vector redirection")
    print()
    
    # Create the NMI vector tester
    tester = NMIVectorTest()
    
    try:
        # Generate the NMI vector test
        success = tester.create_nmi_vector_test("super_metroid_nmi_vector_test.ips")
        
        if success:
            print("\n🎉 SUCCESS! NMI vector redirection test ready!")
            print("\n⚡ THE GOLD STANDARD TEST:")
            print("   → Uses NMI vector redirection (safest method)")
            print("   → Executes during VBlank (perfect timing)")
            print("   → Tests ALL methods: CGRAM, WRAM, sprites")
            print("   → Should work without crashes!")
            return 0
        else:
            print("\n❌ Failed to create NMI vector test")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())