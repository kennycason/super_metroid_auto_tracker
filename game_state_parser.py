#!/usr/bin/env python3
"""
Super Metroid Game State Parser

Extracts game state parsing logic into a testable class.
Takes raw memory data and converts it to structured game state.
"""

import struct
import logging
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)

class SuperMetroidGameStateParser:
    """Parses raw Super Metroid memory data into structured game state"""
    
    def __init__(self):
        # Super Metroid memory layout
        self.memory_map = {
            'health': 0x7E09C2,
            'max_health': 0x7E09C4,
            'missiles': 0x7E09C6,
            'max_missiles': 0x7E09C8,
            'supers': 0x7E09CA,
            'max_supers': 0x7E09CC,
            'power_bombs': 0x7E09CE,
            'max_power_bombs': 0x7E09D0,
            'reserve_energy': 0x7E09D6,
            'max_reserve_energy': 0x7E09D4,
            'room_id': 0x7E079B,
            'area_id': 0x7E079F,
            'game_state': 0x7E0998,
            'player_x': 0x7E0AF6,
            'player_y': 0x7E0AFA,
            'items': 0x7E09A4,
            'beams': 0x7E09A8,
            'bosses': 0x7ED828,
        }
        
        # Area names mapping
        self.areas = {
            0: "Crateria", 1: "Brinstar", 2: "Norfair", 
            3: "Wrecked Ship", 4: "Maridia", 5: "Tourian"
        }
    
    def parse_basic_stats(self, stats_data: bytes) -> Dict[str, Any]:
        """Parse basic stats from 22-byte health block"""
        if not stats_data or len(stats_data) < 22:
            return {}
            
        return {
            'health': struct.unpack('<H', stats_data[0:2])[0],
            'max_health': struct.unpack('<H', stats_data[2:4])[0],
            'missiles': struct.unpack('<H', stats_data[4:6])[0],
            'max_missiles': struct.unpack('<H', stats_data[6:8])[0],
            'supers': struct.unpack('<H', stats_data[8:10])[0],
            'max_supers': struct.unpack('<H', stats_data[10:12])[0],
            'power_bombs': struct.unpack('<H', stats_data[12:14])[0],
            'max_power_bombs': struct.unpack('<H', stats_data[14:16])[0],
            'max_reserve_energy': struct.unpack('<H', stats_data[18:20])[0],
            'reserve_energy': struct.unpack('<H', stats_data[20:22])[0],
        }
    
    def parse_location_data(self, room_id_data: bytes, area_id_data: bytes, 
                           game_state_data: bytes, player_x_data: bytes, 
                           player_y_data: bytes) -> Dict[str, Any]:
        """Parse location and position data"""
        location = {}
        
        if room_id_data and len(room_id_data) >= 2:
            location['room_id'] = struct.unpack('<H', room_id_data)[0]
        else:
            location['room_id'] = 0
            
        if area_id_data and len(area_id_data) >= 1:
            area_id = area_id_data[0]
            location['area_id'] = area_id
            location['area_name'] = self.areas.get(area_id, "Unknown")
        else:
            location['area_id'] = 0
            location['area_name'] = "Unknown"
            
        if game_state_data and len(game_state_data) >= 2:
            location['game_state'] = struct.unpack('<H', game_state_data)[0]
        else:
            location['game_state'] = 0
            
        if player_x_data and len(player_x_data) >= 2:
            location['player_x'] = struct.unpack('<H', player_x_data)[0]
        else:
            location['player_x'] = 0
            
        if player_y_data and len(player_y_data) >= 2:
            location['player_y'] = struct.unpack('<H', player_y_data)[0]
        else:
            location['player_y'] = 0
            
        return location
    
    def parse_items(self, items_data: bytes) -> Dict[str, bool]:
        """Parse item collection status"""
        if not items_data or len(items_data) < 2:
            return {}
            
        items_value = struct.unpack('<H', items_data)[0]
        return {
            "morph": bool(items_value & 0x0004),
            "bombs": bool(items_value & 0x1000),
            "varia": bool(items_value & 0x0001),
            "gravity": bool(items_value & 0x0020),
            "hijump": bool(items_value & 0x0100),
            "speed": bool(items_value & 0x2000),
            "space": bool(items_value & 0x0200),
            "screw": bool(items_value & 0x0008),
            "spring": bool(items_value & 0x0002),
            "xray": bool(items_value & 0x8000),
            "grapple": bool(items_value & 0x4000)
        }
    
    def parse_beams(self, beams_data: bytes) -> Dict[str, bool]:
        """Parse beam weapon status"""
        if not beams_data or len(beams_data) < 2:
            return {}
            
        beams_value = struct.unpack('<H', beams_data)[0]
        return {
            "charge": bool(beams_value & 0x1000),
            "ice": bool(beams_value & 0x0002),
            "wave": bool(beams_value & 0x0001),
            "spazer": bool(beams_value & 0x0004),
            "plasma": bool(beams_value & 0x0008)
        }
    
    def parse_bosses(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any] = None) -> Dict[str, Any]:
        """Parse boss defeat status with advanced detection logic"""
        if not boss_memory_data:
            return {}
            
        bosses = {}
        
        # Basic boss flags
        main_bosses_data = boss_memory_data.get('main_bosses')
        if main_bosses_data and len(main_bosses_data) >= 2:
            bosses_value = struct.unpack('<H', main_bosses_data)[0]
            bosses.update({
                'bomb_torizo': bool(bosses_value & 0x04),
                'kraid': bool(bosses_value & 0x100),
                'spore_spawn': bool(bosses_value & 0x200),
                'draygon': bool(bosses_value & 0x1000),
                'mother_brain': bool(bosses_value & 0x01)
            })
        
        # Advanced boss detection
        crocomire_data = boss_memory_data.get('crocomire')
        if crocomire_data and len(crocomire_data) >= 2:
            crocomire_value = struct.unpack('<H', crocomire_data)[0]
            bosses['crocomire'] = bool(crocomire_value & 0x02) and (crocomire_value >= 0x0202)
        else:
            bosses['crocomire'] = False
            
        # Multi-address boss scans for complex bosses
        boss_scan_results = {}
        for key, data in boss_memory_data.items():
            if key.startswith('boss_plus_') and data and len(data) >= 2:
                boss_scan_results[key] = struct.unpack('<H', data)[0]
        
        # Fixed boss detection logic (copied from working unified server)
        phantoon_addr = boss_scan_results.get('boss_plus_3', 0)
        phantoon_detected = phantoon_addr and (phantoon_addr & 0x01)  # Fixed: removed 0x0300 requirement
        bosses['phantoon'] = phantoon_detected
        
        # Botwoon detection
        botwoon_addr_1 = boss_scan_results.get('boss_plus_2', 0)
        botwoon_addr_2 = boss_scan_results.get('boss_plus_4', 0)
        botwoon_detected = ((botwoon_addr_1 & 0x04) and (botwoon_addr_1 > 0x0100)) or \
                          ((botwoon_addr_2 & 0x02) and (botwoon_addr_2 > 0x0001))
        bosses['botwoon'] = botwoon_detected
        
        # Draygon detection - Fixed to detect the 0x0301 pattern
        boss_plus_3_val = boss_scan_results.get('boss_plus_3', 0)
        if boss_plus_3_val == 0x0301:  # Specific Draygon defeat pattern
            draygon_detected = True
        else:
            # Fallback: original logic for other patterns
            draygon_detected = False
            for val in boss_scan_results.values():
                if val and (val & 0x04):
                    draygon_detected = True
                    break
        bosses['draygon'] = draygon_detected
        
        # Ridley detection - Fixed to check correct memory addresses
        ridley_addr_2 = boss_scan_results.get('boss_plus_2', 0)
        ridley_addr_4 = boss_scan_results.get('boss_plus_4', 0)
        ridley_detected = ((ridley_addr_2 & 0x0001) != 0) or ((ridley_addr_4 & 0x0001) != 0)
        bosses['ridley'] = ridley_detected
        
        # Golden Torizo detection - Fixed threshold to detect 0x0603 pattern
        gt_addr_1 = boss_scan_results.get('boss_plus_1', 0)
        gt_addr_2 = boss_scan_results.get('boss_plus_2', 0)
        condition1 = ((gt_addr_1 & 0x0700) and (gt_addr_1 & 0x0003) and (gt_addr_1 >= 0x0603))  # Lowered from 0x0703
        condition2 = (gt_addr_2 & 0x0100) and (gt_addr_2 >= 0x0500)
        # Removed condition3 that was triggering on Draygon's 0x0301 pattern
        golden_torizo_detected = condition1 or condition2
        bosses['golden_torizo'] = golden_torizo_detected
        
        # Mother Brain detection - Handle both complete sequence AND intermediate fight states
        main_mb_detected = bosses.get('mother_brain', False)  # From original detection above
        
        # Mother Brain has 3 phases: MB1 (glass tank), MB2 (mech body), MB3 (final form)
        # We need to detect intermediate states during the active fight sequence
        
        # Check if we're in the Mother Brain room during the fight
        area_id = location_data.get('area_id', 0) if location_data else 0
        room_id = location_data.get('room_id', 0) if location_data else 0
        in_mb_room = (area_id == 5 and room_id == 56664)  # Tourian Mother Brain room
        
        if main_mb_detected:
            # Complete sequence: all phases are done
            mb1_detected = True
            mb2_detected = True
        elif in_mb_room:
            # We're in Mother Brain room - detect intermediate states
            # Use memory patterns to determine which phases have been completed during the fight
            mb_progress_val = boss_scan_results.get('boss_plus_1', 0)
            
            # If we're in MB room with certain patterns, MB1 must be defeated to get there
            # Look for patterns indicating progression beyond MB1
            if mb_progress_val > 0x0200 or location_data.get('missiles', 1) == 0:
                # Strong indicators that we're past MB1 (fighting MB2/MB3)
                mb1_detected = True
                # MB2 should only be detected as defeated AFTER actually completing her
                # During intermediate fight, don't mark MB2 as defeated yet
                mb2_detected = False  # Conservative: only detect when complete sequence is done
            else:
                # Still fighting MB1
                mb1_detected = False
                mb2_detected = False
        else:
            # Not in Mother Brain room and main bit not set - no phases defeated
            mb1_detected = False
            mb2_detected = False
            
        bosses['mother_brain_1'] = mb1_detected
        bosses['mother_brain_2'] = mb2_detected
        # mother_brain stays as originally detected (complete sequence)
        
        return bosses
    
    def parse_complete_game_state(self, memory_data: Dict[str, bytes]) -> Dict[str, Any]:
        """Parse all memory data into complete game state"""
        try:
            game_state = {}
            
            # Basic stats
            stats_data = memory_data.get('basic_stats')
            if stats_data:
                game_state.update(self.parse_basic_stats(stats_data))
            
            # Location data
            location_data = self.parse_location_data(
                memory_data.get('room_id'),
                memory_data.get('area_id'), 
                memory_data.get('game_state'),
                memory_data.get('player_x'),
                memory_data.get('player_y')
            )
            game_state.update(location_data)
            
            # Items and beams
            game_state['items'] = self.parse_items(memory_data.get('items'))
            game_state['beams'] = self.parse_beams(memory_data.get('beams'))
            
            # Bosses (pass all boss-related memory data)
            boss_memory = {k: v for k, v in memory_data.items() 
                          if k.startswith('boss') or k == 'main_bosses' or k == 'crocomire'}
            game_state['bosses'] = self.parse_bosses(boss_memory, game_state)
            
            return game_state
            
        except Exception as e:
            logger.error(f"Error parsing game state: {e}")
            return {}
    
    def is_valid_game_state(self, game_state: Dict[str, Any]) -> bool:
        """Check if parsed game state looks valid"""
        if not game_state:
            return False
            
        # Basic sanity checks
        if 'health' not in game_state or 'max_health' not in game_state:
            return False
            
        health = game_state.get('health', 0)
        max_health = game_state.get('max_health', 0)
        
        # Health should be reasonable
        if health < 0 or max_health < 0 or health > 1999 or max_health > 1999:
            return False
            
        return True 