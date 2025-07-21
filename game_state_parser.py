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
        player_x = location_data.get('player_x', 0) if location_data else 0
        player_y = location_data.get('player_y', 0) if location_data else 0
        in_mb_room = (area_id == 5 and room_id == 56664)  # Tourian Mother Brain room
        
        if main_mb_detected:
            # Complete sequence: all phases are done
            mb1_detected = True
            mb2_detected = True
        elif in_mb_room:
            # Multi-indicator detection system: use memory patterns + position + ammo as fallback
            mb_progress_val = boss_scan_results.get('boss_plus_1', 0)
            mb_alt_pattern = boss_scan_results.get('boss_plus_2', 0)
            mb_extra_pattern = boss_scan_results.get('boss_plus_3', 0)
            mb_pattern_4 = boss_scan_results.get('boss_plus_4', 0)
            mb_pattern_5 = boss_scan_results.get('boss_plus_5', 0)
            
            # Position-based indicators: where in MB room
            in_mb_fight_area = (player_x >= 700 and player_x <= 2000 and player_y >= 300)
            
            # Game state validation (active gameplay should be 0x000B according to ChatGPT)
            game_state_val = location_data.get('game_state', 0) if location_data else 0
            in_active_gameplay = (game_state_val == 0x000B)
            
            # Missile usage as supporting evidence (not primary)
            initial_missiles = location_data.get('max_missiles', 135) if location_data else 135
            current_missiles = location_data.get('missiles', 135) if location_data else 135
            missiles_used = initial_missiles - current_missiles
            significant_ammo_used = missiles_used >= 35  # Increased from 30 to fix early detection
            
            # Log all memory values for debugging
            logger.info(f"MB Debug - Position: ({player_x}, {player_y}), Missiles: {current_missiles}/{initial_missiles}")
            logger.info(f"MB Debug - Game State: 0x{game_state_val:04X}, In Fight Area: {in_mb_fight_area}")
            logger.info(f"MB Debug - boss_plus_1: 0x{mb_progress_val:04X}, boss_plus_2: 0x{mb_alt_pattern:04X}")
            logger.info(f"MB Debug - boss_plus_3: 0x{mb_extra_pattern:04X}, boss_plus_4: 0x{mb_pattern_4:04X}, boss_plus_5: 0x{mb_pattern_5:04X}")
            
            # CONSERVATIVE but PRACTICAL detection - focus on missile usage as primary indicator
            # Pattern 1: High memory progression value + active gameplay OR good missile evidence
            strong_memory_pattern = ((mb_progress_val >= 0x0700) and in_active_gameplay) or ((mb_progress_val >= 0x0600) and significant_ammo_used)
            
            # Pattern 2: Alternative memory addresses with medium thresholds + evidence
            alt_strong_pattern = (mb_alt_pattern >= 0x0300) and significant_ammo_used
            
            # Pattern 3: Strong missile usage evidence (primary indicator) - increased threshold
            missile_evidence_strong = missiles_used >= 35  # Increased from 30 to fix early detection
            
            # Only detect MB1 if we have strong evidence
            if strong_memory_pattern or alt_strong_pattern or missile_evidence_strong:
                logger.info(f"MB Debug - Detected MB1: strong_mem={strong_memory_pattern}, alt_strong={alt_strong_pattern}, missile_strong={missile_evidence_strong}")
                mb1_detected = True
                
                # MB2 detection - MORE CONSERVATIVE to trigger after hyper beam phase
                # Pattern 1: All missiles used + high memory pattern (stronger requirement)
                all_missiles_used = (current_missiles == 0 and initial_missiles > 0)
                strong_mb2_memory = (mb_pattern_4 >= 0x0300) or (mb_pattern_5 >= 0x0150)  # Higher thresholds
                
                # Pattern 2: Extreme missile usage + strong memory evidence (for hyper beam phase)
                extreme_missile_usage = (missiles_used >= 120) and (mb_progress_val >= 0x0700)  # Much higher thresholds
                
                # Detect MB2 only with VERY strong evidence (after hyper beam phase)
                if (all_missiles_used and strong_mb2_memory) or extreme_missile_usage:
                    logger.info(f"MB Debug - Detected MB2: all_missiles={all_missiles_used}, strong_mb2_memory={strong_mb2_memory}, extreme_usage={extreme_missile_usage}")
                    mb2_detected = True
                else:
                    logger.info(f"MB Debug - MB1 only: insufficient MB2 evidence (pre-hyper beam)")
                    mb2_detected = False
            else:
                logger.info(f"MB Debug - NO MB1 detection: insufficient evidence")
                mb1_detected = False
                mb2_detected = False
        else:
            # Not in Mother Brain room and main bit not set - no phases defeated
            mb1_detected = False
            mb2_detected = False
            
        bosses['mother_brain_1'] = mb1_detected
        bosses['mother_brain_2'] = mb2_detected
        # mother_brain stays as originally detected (complete sequence)
        
        # End-game detection (Samus reaching her ship)
        samus_ship_detected = self._detect_samus_ship(boss_memory_data, location_data, main_mb_detected, mb1_detected, mb2_detected)
        bosses['samus_ship'] = samus_ship_detected
        
        return bosses
    
    def _detect_samus_ship(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any], 
                          main_mb_complete: bool, mb1_complete: bool, mb2_complete: bool) -> bool:
        """Detect when Samus has reached her ship (end-game completion)"""
        if not location_data:
            return False
            
        area_id = location_data.get('area_id', 0)
        room_id = location_data.get('room_id', 0)
        player_x = location_data.get('player_x', 0)
        player_y = location_data.get('player_y', 0)
        
        # Must be in Crateria (area 0) for ship location
        if area_id != 0:
            return False
            
        # Check if Mother Brain sequence is complete (all phases defeated)
        # This ensures we're in the post-game/escape sequence phase
        mother_brain_complete = main_mb_complete or (mb1_complete and mb2_complete)
        
        if not mother_brain_complete:
            return False
            
        # Landing Site room detection (approximate room ID conversion)
        # The SMILE ID 791F8 converts to a runtime room ID we need to check
        # Based on research, Landing Site is typically room ID around 31800-32000 range in decimal
        # But since room IDs can vary, we'll use a more flexible approach
        
        # Check if we're in the general Landing Site area (Crateria, western section)
        # Based on the room layout, Landing Site is in "West of the Lake" section
        # We'll check for reasonable room ID ranges and position within Crateria
        
        # Room ID ranges that could indicate Landing Site area (converted from hex ranges)
        # This covers the general Crateria "West of the Lake" area where the ship is located
        landing_site_room_range = [
            (31224, 31260),  # 0x791F8 area converted to decimal range
            (31000, 31500),  # Broader range to account for room ID variations
        ]
        
        in_landing_area = any(start <= room_id <= end for start, end in landing_site_room_range)
        
        # Also check by position - Landing Site has specific coordinates
        # The ship is typically in the upper portion of the Landing Site room
        # X coordinate around 400-600, Y coordinate around 100-300 (approximate)
        near_ship_position = (300 <= player_x <= 700) and (50 <= player_y <= 400)
        
        # Alternative: check if we're in any Crateria room that could be post-escape
        # Since room IDs can be complex, also accept being in Crateria with MB complete
        # and reasonable positioning (not underground, not too far east/west)
        in_surface_crateria = (area_id == 0 and 
                              200 <= player_x <= 800 and  # Reasonable X range
                              50 <= player_y <= 500)      # Surface level Y range
        
        # End-game is detected if:
        # 1. Mother Brain is defeated AND
        # 2. We're in Crateria AND
        # 3. Either in the Landing Site area OR near ship position OR in surface Crateria
        end_game_detected = (mother_brain_complete and 
                           area_id == 0 and 
                           (in_landing_area or near_ship_position or in_surface_crateria))
        
        if end_game_detected:
            logger.info(f"End-game detected: MB complete, Crateria area, room_id={room_id}, pos=({player_x},{player_y})")
            
        return end_game_detected
    
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