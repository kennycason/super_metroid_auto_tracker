#!/usr/bin/env python3
"""
TEST FOR USER'S EXACT SCENARIO
Based on the user's actual API response showing false negatives
"""

import unittest
import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from game_state_parser import SuperMetroidGameStateParser


class TestUserExactCase(unittest.TestCase):
    
    def setUp(self):
        self.parser = SuperMetroidGameStateParser()
    
    def test_user_exact_api_response_case(self):
        """
        TEST THE USER'S EXACT CURRENT STATE FROM API RESPONSE
        
        User API shows:
        - Tourian (area 5), room 56664 (MB room)
        - 0 missiles, 0 supers, 0 power_bombs 
        - All items collected, plasma beam = true
        - Health: 959/999
        - But mother_brain_1: false, mother_brain_2: false ❌
        
        This is clearly a post-MB2 escape state and should show both true
        """
        print("\n🎯 Testing USER'S EXACT CURRENT STATE")
        
        # Based on user's API response
        boss_memory_data = {
            'main_bosses': b'\x04\x03',      # 0x0304 - normal bosses detected
            'boss_plus_1': b'\x03\x07',      # 0x0703 - likely post-MB signature
            'boss_plus_2': b'\x07\x01',      # 0x0107
            'boss_plus_3': b'\x01\x03',      # 0x0301
            'boss_plus_4': b'\x03\x00',      # 0x0003
            'crocomire': b'\x03\x02',        # 0x0203
        }
        
        # User's exact location and stats
        location_data = {
            'area_id': 5,            # Tourian
            'room_id': 56664,        # Mother Brain room
            'game_state': 15,        # From user's API
            'player_x': 233,         # From user's API
            'player_y': 183,         # From user's API
            'health': 959,           # From user's API
            'max_health': 999,       # From user's API
            'missiles': 0,           # ⚠️ ALL AMMO SPENT - post-fight indicator
            'max_missiles': 135,
            'supers': 0,             # ⚠️ ALL AMMO SPENT
            'max_supers': 30,
            'power_bombs': 0,        # ⚠️ ALL AMMO SPENT
            'max_power_bombs': 20,
        }
        
        # Add item data showing full completion
        item_completion = {
            'morph': True,
            'bombs': True,
            'varia': True,
            'gravity': True,
            'hijump': True,
            'speed': True,
            'space': True,
            'screw': True,
            'spring': True,
            'xray': True,
            'grapple': True,
            'charge': True,
            'ice': True,
            'wave': True,
            'spazer': True,
            'plasma': True,  # ⚠️ Has hyper beam equivalent
            'hyper': False,
        }
        
        result = self.parser.parse_bosses(boss_memory_data, location_data)
        
        print(f"  Location: Area {location_data['area_id']} (Tourian), Room {location_data['room_id']} (MB room)")
        print(f"  Health: {location_data['health']}/{location_data['max_health']}")
        print(f"  Ammo: {location_data['missiles']}/{location_data['supers']}/{location_data['power_bombs']} (ALL SPENT)")
        print(f"  Items: All collected + plasma beam")
        print(f"  Expected: MB1=True, MB2=True (clear post-escape state)")
        print(f"  Actual: MB1={result.get('mother_brain_1')}, MB2={result.get('mother_brain_2')}")
        
        # This should DEFINITELY be detected as both MB phases complete
        self.assertTrue(result.get('mother_brain_1', False), 
            "Post-escape state in Tourian with 0 ammo should show MB1 complete")
        self.assertTrue(result.get('mother_brain_2', False), 
            "Post-escape state in Tourian with 0 ammo should show MB2 complete")
    
    def test_comprehensive_post_escape_detection(self):
        """
        TEST COMPREHENSIVE POST-ESCAPE DETECTION
        Multiple signatures that should trigger post-escape detection
        """
        print("\n🚀 Testing COMPREHENSIVE POST-ESCAPE DETECTION")
        
        # Test different memory signatures that indicate post-escape
        test_cases = [
            {
                'name': 'Signature 0x0703 in Tourian',
                'boss_memory': {
                    'main_bosses': b'\x04\x03',      # Normal bosses
                    'boss_plus_1': b'\x03\x07',      # 0x0703
                    'boss_plus_2': b'\x07\x01',      # 0x0107
                    'boss_plus_3': b'\x01\x03',      # 0x0301
                    'boss_plus_4': b'\x03\x00',      # 0x0003
                    'crocomire': b'\x03\x02',        # 0x0203
                }
            },
            {
                'name': 'Main boss bit set',
                'boss_memory': {
                    'main_bosses': b'\x05\x03',      # 0x0305 - main MB bit set
                    'boss_plus_1': b'\x03\x07',      # 0x0703
                    'boss_plus_2': b'\x07\x01',      # 0x0107
                    'boss_plus_3': b'\x01\x03',      # 0x0301
                    'boss_plus_4': b'\x03\x00',      # 0x0003
                    'crocomire': b'\x03\x02',        # 0x0203
                }
            }
        ]
        
        for case in test_cases:
            print(f"\n  Testing: {case['name']}")
            
            location_data = {
                'area_id': 5,            # Tourian
                'room_id': 56664,        # MB room
                'missiles': 0,           # No ammo = post-fight
                'supers': 0,
                'power_bombs': 0,
                'health': 959,
            }
            
            result = self.parser.parse_bosses(case['boss_memory'], location_data)
            
            print(f"    Result: MB1={result.get('mother_brain_1')}, MB2={result.get('mother_brain_2')}")
            
            # Both should be true for post-escape scenarios
            self.assertTrue(result.get('mother_brain_1', False), 
                f"{case['name']} should detect MB1 complete")
            self.assertTrue(result.get('mother_brain_2', False), 
                f"{case['name']} should detect MB2 complete")
    
    def test_debug_memory_parsing(self):
        """
        TEST DEBUG MEMORY PARSING
        Print detailed debug info to understand what's happening
        """
        print("\n🔍 DEBUGGING MEMORY PARSING")
        
        # User's current memory signatures
        boss_memory_data = {
            'main_bosses': b'\x04\x03',      # 0x0304
            'boss_plus_1': b'\x03\x07',      # 0x0703
            'boss_plus_2': b'\x07\x01',      # 0x0107
            'boss_plus_3': b'\x01\x03',      # 0x0301
            'boss_plus_4': b'\x03\x00',      # 0x0003
            'crocomire': b'\x03\x02',        # 0x0203
        }
        
        location_data = {
            'area_id': 5,
            'room_id': 56664,
            'missiles': 0,
            'health': 959,
        }
        
        # Parse each signature separately to see what's detected
        main_bosses = int.from_bytes(boss_memory_data['main_bosses'], byteorder='little')
        boss_plus_1 = int.from_bytes(boss_memory_data['boss_plus_1'], byteorder='little')
        boss_plus_2 = int.from_bytes(boss_memory_data['boss_plus_2'], byteorder='little')
        
        print(f"  main_bosses: 0x{main_bosses:04X} ({main_bosses})")
        print(f"  boss_plus_1: 0x{boss_plus_1:04X} ({boss_plus_1})")
        print(f"  boss_plus_2: 0x{boss_plus_2:04X} ({boss_plus_2})")
        print(f"  MB bit in main_bosses: {bool(main_bosses & 0x01)}")
        print(f"  Location: Area {location_data['area_id']}, Room {location_data['room_id']}")
        
        result = self.parser.parse_bosses(boss_memory_data, location_data)
        
        print(f"  Final result: MB1={result.get('mother_brain_1')}, MB2={result.get('mother_brain_2')}")


def run_user_exact_tests():
    """Run the user's exact scenario tests"""
    print("🎯 USER'S EXACT CASE TESTS")
    print("=" * 50)
    print("Testing the exact scenario from user's API response")
    print("=" * 50)
    
    suite = unittest.TestLoader().loadTestsFromTestCase(TestUserExactCase)
    runner = unittest.TextTestRunner(verbosity=2, stream=sys.stdout)
    result = runner.run(suite)
    
    print("\n" + "=" * 50)
    print("🎯 USER EXACT CASE TEST SUMMARY")
    print("=" * 50)
    print(f"Tests run: {result.testsRun}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    
    success_rate = ((result.testsRun - len(result.failures) - len(result.errors)) / result.testsRun) * 100
    print(f"📊 Success Rate: {success_rate:.1f}%")
    
    if success_rate >= 95:
        print("🎉 EXCELLENT! User's case is now working.")
    else:
        print("🚨 STILL BROKEN! User will continue seeing false negatives.")
        if result.failures:
            print("\n❌ FAILURES:")
            for test, traceback in result.failures:
                print(f"  - {test}")
    
    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_user_exact_tests()
    sys.exit(0 if success else 1) 