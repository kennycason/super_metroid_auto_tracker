#!/usr/bin/env python3
"""
MEMORY SIGNATURE DEBUG SCRIPT
Test what memory signatures and beam values are actually being detected
"""

import requests
import time

def test_memory_detection():
    print("🔍 MEMORY SIGNATURE DEBUG TEST")
    print("=" * 50)
    
    try:
        # Get current API data
        response = requests.get("http://localhost:8000/api/status")
        data = response.json()
        
        print("📊 CURRENT API DATA:")
        print(f"  Connected: {data.get('connected')}")
        print(f"  Game Loaded: {data.get('game_loaded')}")
        print(f"  Area: {data['stats']['area_id']} ({data['stats']['area_name']})")
        print(f"  Room: {data['stats']['room_id']}")
        print(f"  Position: ({data['stats']['player_x']}, {data['stats']['player_y']})")
        print()
        
        print("🔫 BEAM STATUS:")
        beams = data['stats']['beams']
        for beam, status in beams.items():
            print(f"  {beam}: {status}")
        print()
        
        print("👾 BOSS STATUS:")
        bosses = data['stats']['bosses']
        for boss, status in bosses.items():
            print(f"  {boss}: {status}")
        print()
        
        print("🎯 KEY INDICATORS:")
        print(f"  MB1 Complete: {bosses.get('mother_brain_1', False)}")
        print(f"  MB2 Complete: {bosses.get('mother_brain_2', False)}")
        print(f"  Hyper Beam: {beams.get('hyper', False)}")
        print(f"  Plasma Beam: {beams.get('plasma', False)}")
        print(f"  Missiles: {data['stats']['missiles']}/{data['stats']['max_missiles']}")
        print()
        
        # Analyze the state
        has_all_major_items = all([
            beams.get('charge', False),
            beams.get('ice', False), 
            beams.get('wave', False),
            beams.get('spazer', False),
            beams.get('plasma', False)
        ])
        
        in_tourian = data['stats']['area_id'] == 5
        zero_missiles = data['stats']['missiles'] == 0
        
        print("🧠 STATE ANALYSIS:")
        print(f"  Has all major beams: {has_all_major_items}")
        print(f"  In Tourian: {in_tourian}")
        print(f"  Zero missiles: {zero_missiles}")
        print(f"  → Expected post-game state: {has_all_major_items and zero_missiles}")
        print()
        
        if has_all_major_items and zero_missiles and not beams.get('hyper', False):
            print("🚨 HYPER BEAM DETECTION FAILURE!")
            print("   All major beams + zero missiles = should have Hyper Beam")
            print("   This indicates Hyper Beam detection is broken")
        
        if not (bosses.get('mother_brain_1', False) and bosses.get('mother_brain_2', False)):
            print("🚨 MOTHER BRAIN DETECTION FAILURE!")
            print("   Post-game state detected but MB phases not complete")
            print("   This indicates MB detection logic is broken")
            
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    test_memory_detection() 