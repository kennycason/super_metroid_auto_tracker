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
        self.logger = logging.getLogger(__name__)
        # Persistent state for Mother Brain phases - once detected, stays detected
        self.mother_brain_phase_state = {
            'mb1_detected': False,
            'mb2_detected': False
        }
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
    
    def _is_intro_scene(self, location_data: Optional[Dict[str, Any]] = None, health: int = 0) -> bool:
        """
        Detect if we're in the intro scene where items shouldn't show as activated.
        Conservative detection to avoid false positives.
        """
        if not location_data:
            return False
            
        area_id = location_data.get('area_id', 0)
        room_id = location_data.get('room_id', 0)
        
        # Intro scene indicators (conservative approach):
        # 1. Very early Crateria (starting area) with minimal progress
        # 2. Low health (starting health range)  
        # 3. Early room IDs that indicate intro/opening sequence
        
        in_starting_area = (area_id == 0)  # Crateria
        has_starting_health = (health <= 99)  # Starting health or lower
        in_intro_rooms = (room_id < 1000 or room_id == 0)  # Very early room IDs or invalid data
        
        # Only consider it intro if ALL conditions suggest early game
        is_intro = (in_starting_area and has_starting_health and in_intro_rooms)
        
        if is_intro:
            logger.info(f"🎬 INTRO SCENE DETECTED: Area={area_id}, Room={room_id}, Health={health} - filtering items")
            
        return is_intro

    def parse_basic_stats(self, stats_data: bytes, location_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Parse basic stats from 22-byte health block"""
        if not stats_data or len(stats_data) < 22:
            return {}
        
        # Parse individual values
        health = struct.unpack('<H', stats_data[0:2])[0]
        max_health = struct.unpack('<H', stats_data[2:4])[0]
        missiles = struct.unpack('<H', stats_data[4:6])[0]
        max_missiles = struct.unpack('<H', stats_data[6:8])[0]
        supers = struct.unpack('<H', stats_data[8:10])[0]
        max_supers = struct.unpack('<H', stats_data[10:12])[0]
        power_bombs = struct.unpack('<H', stats_data[12:14])[0]
        max_power_bombs = struct.unpack('<H', stats_data[14:16])[0]
        max_reserve_energy = struct.unpack('<H', stats_data[18:20])[0]
        reserve_energy = struct.unpack('<H', stats_data[20:22])[0]
        
        # Check if we're in intro scene - if so, filter non-energy items
        is_intro = self._is_intro_scene(location_data, health)
        
        if is_intro:
            # During intro: preserve health/energy, zero out missiles/supers/power bombs
            missiles = 0
            max_missiles = 0
            supers = 0
            max_supers = 0
            power_bombs = 0
            max_power_bombs = 0
        
        return {
            'health': health,
            'max_health': max_health,
            'missiles': missiles,
            'max_missiles': max_missiles,
            'supers': supers,
            'max_supers': max_supers,
            'power_bombs': power_bombs,
            'max_power_bombs': max_power_bombs,
            'max_reserve_energy': max_reserve_energy,
            'reserve_energy': reserve_energy,
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
            location['area_name'] = self.areas.get(area_id, "")
        else:
            location['area_id'] = 0
            location['area_name'] = ""
            
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
    
    def parse_items(self, items_data: bytes, location_data: Optional[Dict[str, Any]] = None, health: int = 0) -> Dict[str, bool]:
        """Parse item collection status"""
        if not items_data or len(items_data) < 2:
            return {}
            
        items_value = struct.unpack('<H', items_data)[0]
        
        # Check if we're in intro scene - if so, filter all items to False
        is_intro = self._is_intro_scene(location_data, health)
        
        if is_intro:
            # During intro: all items should show as not collected
            return {
                "morph": False,
                "bombs": False,
                "varia": False,
                "gravity": False,
                "hijump": False,
                "speed": False,
                "space": False,
                "screw": False,
                "spring": False,
                "xray": False,
                "grapple": False
            }
        
        # Normal parsing when not in intro
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
    
    def parse_beams(self, beams_data: bytes, location_data: Optional[Dict[str, Any]] = None, health: int = 0) -> Dict[str, bool]:
        """Parse beam weapon status"""
        if not beams_data or len(beams_data) < 2:
            return {}
            
        beams_value = struct.unpack('<H', beams_data)[0]
        
        # Always log raw beam value for debugging
        logger.info(f"🔍 Raw beam value: 0x{beams_value:04X} ({beams_value})")
        
        beams = {
            "charge": bool(beams_value & 0x1000),
            "ice": bool(beams_value & 0x0002),
            "wave": bool(beams_value & 0x0001),
            "spazer": bool(beams_value & 0x0004),
            "plasma": bool(beams_value & 0x0008)
        }
        
        # HYPER BEAM DETECTION - Test multiple bit positions
        # Hyper beam is only given after MB1 is defeated, making it a perfect indicator
        hyper_beam_bits = [0x0010, 0x0020, 0x0040, 0x0080, 0x0100, 0x0200, 0x0400, 0x0800]
        hyper_detected = False
        
        for bit in hyper_beam_bits:
            if beams_value & bit:
                hyper_detected = True
                logger.info(f"🌟 HYPER BEAM detected at bit position 0x{bit:04X} (value: 0x{beams_value:04X})")
                break
        
        # If not detected with common bits, check if any unknown bits are set
        known_bits = 0x1000 | 0x0002 | 0x0001 | 0x0004 | 0x0008  # charge, ice, wave, spazer, plasma
        unknown_bits = beams_value & ~known_bits
        if unknown_bits and not hyper_detected:
            logger.info(f"🔍 Unknown beam bits detected: 0x{unknown_bits:04X} - could be hyper beam")
            # Try treating any unknown bit as hyper beam
            hyper_detected = True
        
        beams["hyper"] = hyper_detected
        
        # Check if we're in intro scene - if so, filter beams (but allow charge beam)
        is_intro = self._is_intro_scene(location_data, health)
        
        if is_intro:
            # During intro: only allow charge beam (player starts with it), filter others
            beams = {
                "charge": beams["charge"],  # Keep charge beam as-is
                "ice": False,
                "wave": False,
                "spazer": False,
                "plasma": False,
                "hyper": False
            }
        
        # Log final beam analysis
        logger.info(f"🔍 Beam analysis: charge={beams['charge']}, ice={beams['ice']}, wave={beams['wave']}, spazer={beams['spazer']}, plasma={beams['plasma']}, hyper={beams['hyper']}")
        
        return beams
    
    def parse_bosses(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any] = None) -> Dict[str, Any]:
        """Parse boss defeat status with advanced detection logic"""
        if not boss_memory_data:
            return {}

        # 🧠 MB Cache Debug - Log current cache state
        cached_mb1 = self.mother_brain_phase_state.get('mb1_detected', False)
        cached_mb2 = self.mother_brain_phase_state.get('mb2_detected', False)
        logger.info(f"🧠 MB Cache: MB1={cached_mb1} MB2={cached_mb2}")

        # 🚨 NUCLEAR MB2 CACHE RESET - Force clear if clearly not in escape sequence
        if location_data:
            area_id = location_data.get('area_id', 0)
            room_id = location_data.get('room_id', 0)
            missiles = location_data.get('missiles', 0)
            max_missiles = location_data.get('max_missiles', 1)
            
            # If in MB room with reasonable missile count, force clear MB2 cache
            in_mb_room_now = (area_id in [5, 10] and room_id == 56664)
            has_reasonable_missiles = (missiles > max_missiles * 0.7) if max_missiles > 0 else False
            
            if in_mb_room_now and has_reasonable_missiles:
                logger.info(f"🚨 NUCLEAR RESET: In MB room with {missiles}/{max_missiles} missiles - force clearing MB2")
                self.mother_brain_phase_state['mb2_detected'] = False
                cached_mb2 = False

        # ❗️1. PREVENT MB CACHE REVERSION - Early return if MB2 permanently cached
        # BUT STILL PARSE OTHER BOSSES! Don't hardcode them to False
        if cached_mb2:
            logger.info("🔒 MB2 permanently cached — skipping MB detection but parsing other bosses")
            # Parse other bosses normally, just skip MB detection later
            pass  # Continue with normal boss parsing

        # CRITICAL: Initialize ALL variables at function start to prevent scope errors
        mb_official_hp = 0  # Initialize immediately to prevent UnboundLocalError
        mb1_transition = False
        mb2_transition = False
        escape_timer_active = False
        has_live_boss_data = False
        detection_method = "none"
        
        # OFFICIAL AUTOSPLITTER MOTHER BRAIN HP EXTRACTION
        # Extract official Mother Brain HP early for use throughout detection
        if boss_memory_data.get('mother_brain_official_hp') and len(boss_memory_data['mother_brain_official_hp']) >= 2:
            try:
                mb_official_hp = struct.unpack('<H', boss_memory_data['mother_brain_official_hp'])[0]
            except (struct.error, TypeError):
                mb_official_hp = 0  # Fallback if unpacking fails
        else:
            mb_official_hp = 0  # Ensure it's always set
        
        # Initialize all escape timer values
        escape_timer_1_val = 0
        escape_timer_2_val = 0
        escape_timer_3_val = 0
        escape_timer_4_val = 0
        escape_timer_5_val = 0
        escape_timer_6_val = 0
        escape_timer_7_val = 0
        escape_timer_8_val = 0
        escape_timer_9_val = 0
        escape_timer_10_val = 0
        escape_timer_11_val = 0
        escape_timer_12_val = 0
        
        # Initialize all boss HP values
        boss_hp_1_val = 0
        boss_hp_2_val = 0
        boss_hp_3_val = 0
        
        # Initialize location variables
        area_id = location_data.get('area_id', 0) if location_data else 0
        room_id = location_data.get('room_id', 0) if location_data else 0
        in_mb_room = (area_id in [5, 10] and room_id == 56664)  # Mother Brain room (areas 5 OR 10)
        
        # Initialize detection flags
        mb1_detected = False
        mb2_detected = False
        
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
        
        # Advanced Mother Brain detection using multiple reliable indicators
        
        # Define all variables used throughout MB detection to avoid scope issues
        # main_mb_detected = bosses.get('mother_brain', False)
        # mb1_detected = False
        # mb2_detected = False
        
        # Get previous HP from cache
        if not hasattr(self, 'previous_mb_hp'):
            self.previous_mb_hp = 0
            
        # OFFICIAL PHASE THRESHOLDS (from autosplitter community)
        PHASE_1_HP = 3000    # 0xBB8
        PHASE_2_HP = 18000   # 0x4650  
        PHASE_3_HP = 36000   # 0x8CA0
        
        logger.info(f"🤖 Official MB HP: Previous={self.previous_mb_hp}, Current={mb_official_hp}")
        
        # SAVE STATE CONTRADICTION DETECTION
        # If we're in MB room with full/high missiles and high health, likely a save state reload
        current_missiles = location_data.get('missiles', 0) if location_data else 0
        max_missiles = location_data.get('max_missiles', 0) if location_data else 0
        current_health = location_data.get('health', 0) if location_data else 0
        max_health = location_data.get('max_health', 0) if location_data else 0
        
        missiles_ratio = current_missiles / max_missiles if max_missiles > 0 else 0
        health_ratio = current_health / max_health if max_health > 0 else 0
        
        if in_mb_room and missiles_ratio >= 0.9 and health_ratio >= 0.85:
            logger.info(f"🔄 SAVE STATE CONTRADICTION detected: {missiles_ratio:.1%} missiles + {health_ratio:.1%} health in MB room")
            logger.info(f"🔄 Likely save state reload - resetting MB detection cache")
            # Force reset regardless of other detection
            mb1_detected = False
            mb2_detected = False
            if hasattr(self, 'mother_brain_phase_state'):
                self.mother_brain_phase_state = {
                    'mb1_detected': False,
                    'mb2_detected': False
                }
                logger.info(f"🗑️ Forced cache reset due to contradiction")
            
        # ESCAPE TIMER APPROACH - Much more reliable than memory patterns
        if boss_memory_data.get('escape_timer_1') and len(boss_memory_data['escape_timer_1']) >= 2:
            escape_timer_1_val = struct.unpack('<H', boss_memory_data['escape_timer_1'])[0]
        if boss_memory_data.get('escape_timer_2') and len(boss_memory_data['escape_timer_2']) >= 2:
            escape_timer_2_val = struct.unpack('<H', boss_memory_data['escape_timer_2'])[0]
        if boss_memory_data.get('escape_timer_3') and len(boss_memory_data['escape_timer_3']) >= 2:
            escape_timer_3_val = struct.unpack('<H', boss_memory_data['escape_timer_3'])[0]
        if boss_memory_data.get('escape_timer_4') and len(boss_memory_data['escape_timer_4']) >= 2:
            escape_timer_4_val = struct.unpack('<H', boss_memory_data['escape_timer_4'])[0]
        if boss_memory_data.get('escape_timer_5') and len(boss_memory_data['escape_timer_5']) >= 2:
            escape_timer_5_val = struct.unpack('<H', boss_memory_data['escape_timer_5'])[0]
        if boss_memory_data.get('escape_timer_6') and len(boss_memory_data['escape_timer_6']) >= 2:
            escape_timer_6_val = struct.unpack('<H', boss_memory_data['escape_timer_6'])[0]
        if boss_memory_data.get('escape_timer_7') and len(boss_memory_data['escape_timer_7']) >= 2:
            escape_timer_7_val = struct.unpack('<H', boss_memory_data['escape_timer_7'])[0]
        if boss_memory_data.get('escape_timer_8') and len(boss_memory_data['escape_timer_8']) >= 2:
            escape_timer_8_val = struct.unpack('<H', boss_memory_data['escape_timer_8'])[0]
        if boss_memory_data.get('escape_timer_9') and len(boss_memory_data['escape_timer_9']) >= 2:
            escape_timer_9_val = struct.unpack('<H', boss_memory_data['escape_timer_9'])[0]
        if boss_memory_data.get('escape_timer_10') and len(boss_memory_data['escape_timer_10']) >= 2:
            escape_timer_10_val = struct.unpack('<H', boss_memory_data['escape_timer_10'])[0]
        if boss_memory_data.get('escape_timer_11') and len(boss_memory_data['escape_timer_11']) >= 2:
            escape_timer_11_val = struct.unpack('<H', boss_memory_data['escape_timer_11'])[0]
        if boss_memory_data.get('escape_timer_12') and len(boss_memory_data['escape_timer_12']) >= 2:
            escape_timer_12_val = struct.unpack('<H', boss_memory_data['escape_timer_12'])[0]

        # Escape timer indicates MB2 completion (timer starts after MB2 dies)
        escape_timer_active = (escape_timer_1_val > 0) or (escape_timer_2_val > 0) or \
                             (escape_timer_3_val > 0) or (escape_timer_4_val > 0) or \
                             (escape_timer_5_val > 0) or (escape_timer_6_val > 0) or \
                             (escape_timer_7_val > 0) or (escape_timer_8_val > 0) or \
                             (escape_timer_9_val > 0) or (escape_timer_10_val > 0) or \
                             (escape_timer_11_val > 0) or (escape_timer_12_val > 0)
        
        # Log all escape timer values for debugging
        logger.info(f"🎯 Enhanced Escape Timer Detection:")
        logger.info(f"   Timer1: {escape_timer_1_val:04X}, Timer2: {escape_timer_2_val:04X}")
        logger.info(f"   Timer3: {escape_timer_3_val:04X}, Timer4: {escape_timer_4_val:04X}")
        logger.info(f"   Timer5: {escape_timer_5_val:04X}, Timer6: {escape_timer_6_val:04X}")
        logger.info(f"   Timer7: {escape_timer_7_val:04X}, Timer8: {escape_timer_8_val:04X}")
        logger.info(f"   Timer9: {escape_timer_9_val:04X}, Timer10: {escape_timer_10_val:04X}")
        logger.info(f"   Timer11: {escape_timer_11_val:04X}, Timer12: {escape_timer_12_val:04X}")
        logger.info(f"   Active: {escape_timer_active}")
        
        # MEMORY SCAN ANALYSIS - Look for any non-zero timers
        scan_found_timers = []
        if boss_memory_data.get('scan_090x'):
            scan_data = boss_memory_data['scan_090x']
            for i in range(0, len(scan_data), 2):
                if i + 1 < len(scan_data):
                    val = struct.unpack('<H', scan_data[i:i+2])[0]
                    if val > 0:
                        addr = 0x7E0900 + i
                        scan_found_timers.append((addr, val))
                        
        if boss_memory_data.get('scan_094x'):
            scan_data = boss_memory_data['scan_094x']
            for i in range(0, len(scan_data), 2):
                if i + 1 < len(scan_data):
                    val = struct.unpack('<H', scan_data[i:i+2])[0]
                    if val > 0:
                        addr = 0x7E0940 + i
                        scan_found_timers.append((addr, val))
                        
        if boss_memory_data.get('scan_09Ex'):
            scan_data = boss_memory_data['scan_09Ex']
            for i in range(0, len(scan_data), 2):
                if i + 1 < len(scan_data):
                    val = struct.unpack('<H', scan_data[i:i+2])[0]
                    if val > 0:
                        addr = 0x7E09E0 + i
                        scan_found_timers.append((addr, val))
        
        if scan_found_timers:
            logger.info(f"🔍 MEMORY SCAN - Found non-zero timers:")
            for addr, val in scan_found_timers:
                logger.info(f"   Address 0x{addr:06X}: {val:04X} ({val})")
                # If any reasonable timer value found, activate escape detection
                if 100 <= val <= 99999:  # Reasonable timer range
                    escape_timer_active = True
                    logger.info(f"🚨 FOUND ESCAPE TIMER at 0x{addr:06X} = {val}!")
        else:
            logger.info(f"🔍 MEMORY SCAN - No non-zero timers found in scanned areas")
        
        # BOSS HP APPROACH - Direct detection via boss health
        if boss_memory_data.get('boss_hp_1') and len(boss_memory_data['boss_hp_1']) >= 2:
            boss_hp_1_val = struct.unpack('<H', boss_memory_data['boss_hp_1'])[0]
        if boss_memory_data.get('boss_hp_2') and len(boss_memory_data['boss_hp_2']) >= 2:
            boss_hp_2_val = struct.unpack('<H', boss_memory_data['boss_hp_2'])[0] 
        if boss_memory_data.get('boss_hp_3') and len(boss_memory_data['boss_hp_3']) >= 2:
            boss_hp_3_val = struct.unpack('<H', boss_memory_data['boss_hp_3'])[0]
        
        # HYPER BEAM APPROACH - TODO: When we find the correct bit
        # hyper_beam_enabled = location_data.get('beams', {}).get('hyper', False) if location_data else False
        
        logger.info(f"🎯 Reliable MB Detection - Escape Timer 1: {escape_timer_1_val:04X}, Timer 2: {escape_timer_2_val:04X}")
        logger.info(f"🩸 Boss HP Detection - HP1: {boss_hp_1_val:04X}, HP2: {boss_hp_2_val:04X}, HP3: {boss_hp_3_val:04X}")
        
        # Check if we're in Mother Brain room for context
        # area_id = location_data.get('area_id', 0) if location_data else 0
        # room_id = location_data.get('room_id', 0) if location_data else 0
        # in_mb_room = (area_id == 5 and room_id == 56664)  # Tourian Mother Brain room
        
        # Define variables used later regardless of detection path
        # main_mb_detected = bosses.get('mother_brain', False)
        # mb1_detected = False
        # mb2_detected = False
        
        # INTEGRATED MOTHER BRAIN DETECTION SYSTEM
        # Primary: Official autosplitter transitions (most reliable)
        # Secondary: Hyper Beam detection (perfect MB1 indicator)
        # Backup: Our existing escape timer + HP analysis systems
        
        # CRITICAL: Capture original cache state BEFORE any modifications
        original_mb1_state = self.mother_brain_phase_state.get('mb1_detected', False)
        original_mb2_state = self.mother_brain_phase_state.get('mb2_detected', False)
        
        logger.info(f"🔄 Original cache state: MB1={original_mb1_state}, MB2={original_mb2_state}")
        
        # ❗️1. CACHE BYPASS - If MB2 is cached, use cached values and skip complex detection
        mb1_detected = False
        mb2_detected = False
        # REMOVED: Early return that was preventing conservative detection
        # if cached_mb2:
        #     logger.info("🔒 MB2 permanently cached — using cached values")
        #     mb1_detected = True  # MB2 implies MB1
        #     mb2_detected = True  # ← This was forcing MB2=True!
        #     detection_method = "permanent_cache"
        
        # HYPER BEAM DETECTION - Perfect MB1 completion indicator
        hyper_beam_detected = False
        if location_data and location_data.get('beams', {}).get('hyper', False):
            hyper_beam_detected = True
            logger.info(f"🌟 HYPER BEAM DETECTED! This confirms MB1 completion")
        
        # Official autosplitter HP transition detection
        if in_mb_room and mb_official_hp > 0:
            mb1_transition = (self.previous_mb_hp == 0 and mb_official_hp == PHASE_2_HP)
            mb2_transition = (self.previous_mb_hp == 0 and mb_official_hp == PHASE_3_HP)
        
        # DETECTION PRIORITY SYSTEM (most reliable first)
        detection_method = "none"
        
        # Skip complex detection if we're using cached values
        if not cached_mb2:
            # 1. OFFICIAL AUTOSPLITTER DETECTION (highest priority)
            if in_mb_room and (mb1_transition or mb2_transition):
                detection_method = "official_transitions"
                if mb1_transition:
                    mb1_detected = True
                    logger.info(f"🏆 MB1 detected via OFFICIAL autosplitter transition")
                if mb2_transition:
                    mb1_detected = True  # MB2 implies MB1 complete
                    mb2_detected = True
                    logger.info(f"🏆 MB2 detected via OFFICIAL autosplitter transition")
        
            # 2. HYPER BEAM DETECTION (high priority backup)
            elif hyper_beam_detected:
                detection_method = "hyper_beam"
                logger.info(f"✨ Hyper Beam detected! MB1 completed.")
                mb1_detected = True
                mb2_detected = True # Hyper beam implies MB2 is also done
        
            # 3. ESCAPE TIMER DETECTION (high priority backup)
            elif escape_timer_active:
                detection_method = "escape_timer"
                logger.info(f"🚨 ESCAPE TIMER DETECTED! MB2 completed, escape sequence active")
                mb1_detected = True  # If MB2 is done, MB1 must be done
                mb2_detected = True
        
            # 3.5. EMERGENCY MB2 DETECTION - Override when escape timer should be active but isn't detected
            elif in_mb_room and 15000 <= boss_hp_3_val <= 40000:
                detection_method = "emergency_mb2"
                logger.info(f"🚨 EMERGENCY MB2 DETECTION: In MB room, HP={boss_hp_3_val}, no escape timer")
                logger.info(f"🚨 This pattern suggests MB2 was just completed but timer not detected")
                mb1_detected = True  # MB2 completion implies MB1 completion
                mb2_detected = True
        
            # 3.6. POST-COMPLETION DETECTION - HP = 0 in MB room indicates MB2 was defeated
            elif in_mb_room and boss_hp_3_val == 0 and (boss_hp_1_val == 0 and boss_hp_2_val == 0):
                detection_method = "post_completion"
                logger.info(f"🏆 POST-COMPLETION DETECTION: In MB room with HP=0 - MB2 was defeated!")
                mb1_detected = True  # MB2 completion implies MB1 completion  
                mb2_detected = True
        
            # 4. LIVE BOSS HP ANALYSIS (when in MB room with HP data)
            elif in_mb_room and (boss_hp_1_val > 0 or boss_hp_2_val > 0 or boss_hp_3_val > 0):
                detection_method = "live_hp_analysis"
                max_hp = max(boss_hp_1_val, boss_hp_2_val, boss_hp_3_val)
                current_hp = boss_hp_3_val if boss_hp_3_val > 0 else max_hp
                
                logger.info(f"🩸 LIVE HP Analysis - Current: {current_hp}")
                
                # MUCH MORE CONSERVATIVE RESET LOGIC
                # Only reset if we're clearly at the very beginning (original pre-fight HP ~41,760)
                is_genuine_reset = (current_hp >= 40000 and current_hp <= 42000)  # Very narrow range
                
                if is_genuine_reset:
                    # This is clearly a save state reload or new attempt
                    logger.info(f"🔄 GENUINE RESET: Original pre-fight HP ({current_hp}) - clearing cache")
                    mb1_detected = False
                    mb2_detected = False
                    self.mother_brain_phase_state = {'mb1_detected': False, 'mb2_detected': False}
                elif current_hp <= 15000:
                    # HP is low enough to indicate MB1 progression
                    logger.info(f"🩸 MB1 active/complete: Current HP {current_hp} <= 15000")
                    mb1_detected = True
                    mb2_detected = (current_hp < 5000) or original_mb2_state  # Preserve MB2 if already detected
                else:
                    # HP is higher but not in reset range - preserve cache state
                    logger.info(f"🔒 PRESERVING CACHE: HP {current_hp} not in reset range, keeping MB1={original_mb1_state}, MB2={original_mb2_state}")
                    mb1_detected = original_mb1_state
                    mb2_detected = original_mb2_state
        
            # 5. SMART FALLBACK (outside MB room - rely heavily on cache)
            else:
                detection_method = "smart_fallback"
                mb_progress_val = boss_scan_results.get('boss_plus_1', 0)
                mb_alt_pattern = boss_scan_results.get('boss_plus_2', 0)
                
                # DEBUG: Log memory values for analysis
                logger.info(f"🧠 Memory Evidence Debug:")
                logger.info(f"   boss_plus_1: 0x{mb_progress_val:04X} ({mb_progress_val})")
                logger.info(f"   boss_plus_2: 0x{mb_alt_pattern:04X} ({mb_alt_pattern})")
                
                # FIXED: Check for Mother Brain memory patterns  
                # 0x0703 can mean DIFFERENT things depending on context:
                # - Outside MB room = Golden Torizo completion  
                # - In MB room + missile usage = MB1 completion
                
                # Context-aware pattern detection
                area_id = location_data.get('area_id', 0) if location_data else 0
                room_id = location_data.get('room_id', 0) if location_data else 0
                
                # Determine if we're in Mother Brain room (area 5 OR 10, room 56664)
                in_mb_room_context = (area_id in [5, 10] and room_id == 56664)
                
                # Context-aware interpretation of 0x0703
                if mb_progress_val == 0x0703:
                    if in_mb_room_context:
                        # If in MB room, it's DEFINITELY MB1 completion (regardless of missile conservation!)
                        logger.info(f"🤖 MB1 COMPLETION DETECTED: 0x0703 in MB room - MB1 defeated!")
                        strong_memory_evidence = True
                    else:
                        # Only if OUTSIDE MB room = Golden Torizo
                        logger.info(f"🥇 GOLDEN TORIZO DETECTED: 0x0703 outside MB room")
                        logger.info(f"🔄 Clearing MB cache due to Golden Torizo false positive")
                        strong_memory_evidence = False
                        # Clear any incorrect MB cache immediately
                        mb1_detected = False
                        mb2_detected = False
                        self.mother_brain_phase_state = {'mb1_detected': False, 'mb2_detected': False}
                elif mb_progress_val >= 0x0704:  # Higher values are likely MB completion
                    strong_memory_evidence = True
                else:
                    strong_memory_evidence = False
                
                # Context-aware detection is now complete
                if strong_memory_evidence:
                    logger.info(f"🔄 Strong memory evidence: 0x{mb_progress_val:04X}")
                    mb1_detected = True
                    
                    # 🚨 CONSERVATIVE MB2 Detection - Only trigger with STRONG evidence
                    # Don't auto-infer MB2 just because MB1 is complete!
                    
                    # MB2 should ONLY be detected with:
                    # 1. Active escape timer (definitive proof)
                    # 2. Or being clearly outside Tourian/MB areas with MB1 done
                    
                    # Get current position for MB2 analysis
                    player_x = location_data.get('player_x', 0) if location_data else 0
                    player_y = location_data.get('player_y', 0) if location_data else 0
                    
                    # Conservative MB2 conditions - MUCH more restrictive
                    clearly_in_escape = escape_timer_active  # Definitive proof
                    clearly_outside_tourian = (area_id not in [5, 10])  # Not in Tourian areas
                    
                    # Only detect MB2 with STRONG evidence
                    if clearly_in_escape:
                        mb2_detected = True
                        logger.info(f"🚨 MB2 DETECTED: Active escape timer - definitive proof!")
                    elif clearly_outside_tourian and mb1_detected and (boss_hp_1_val == 0 and boss_hp_2_val == 0 and boss_hp_3_val == 0):
                        mb2_detected = True
                        logger.info(f"🚨 MB2 DETECTED: Outside Tourian + MB1 done + no boss HP")
                    else:
                        # MB1 complete but NO evidence for MB2 yet
                        logger.info(f"✅ MB1 COMPLETE: Strong evidence, but no MB2 proof yet")
                        # mb2_detected remains False
                        # 🔄 FORCE CLEAR MB2 cache if no evidence supports it
                        if original_mb2_state:
                            logger.info(f"🗑️ CLEARING MB2 CACHE: No evidence supports MB2 completion")
                            mb2_detected = False
                            self.mother_brain_phase_state['mb2_detected'] = False
                else:
                    # No strong memory evidence - rely on cache completely
                    mb1_detected = original_mb1_state
                    mb2_detected = original_mb2_state
                    logger.info(f"🔒 NO EVIDENCE: Preserving cache state MB1={original_mb1_state}, MB2={original_mb2_state}")
        
        # END of "if not cached_mb2:" block - now continue with final assignments
        
        logger.info(f"🎯 Detection method: {detection_method} → MB1={mb1_detected}, MB2={mb2_detected}")
        
        # CRITICAL: MB2 detection should be PERMANENT once achieved
        # Update cache immediately when phases are detected (prevent loss)
        if mb1_detected:
            self.mother_brain_phase_state['mb1_detected'] = True
        if mb2_detected:
            self.mother_brain_phase_state['mb2_detected'] = True
            
        # EMERGENCY MB2 DETECTION: If we ever had MB2=True, keep it unless explicit reset
        cached_mb1 = self.mother_brain_phase_state.get('mb1_detected', False)
        cached_mb2 = self.mother_brain_phase_state.get('mb2_detected', False)
        
        # PERMANENT PERSISTENCE: Once MB2 is achieved, it stays achieved
        # BUT respect when conservative detection explicitly clears cache
        current_mb1_cache = self.mother_brain_phase_state.get('mb1_detected', False)
        current_mb2_cache = self.mother_brain_phase_state.get('mb2_detected', False)
        
        # 🧠 SMART CACHE VALIDATION - Don't blindly trust cache forever!
        # Validate MB2 cache with supporting evidence before using it
        if current_mb2_cache:
            logger.info(f"🔍 VALIDATING MB2 CACHE - checking for supporting evidence...")
            
            # Check for supporting evidence that cache is still valid
            hyper_beam_active = False
            escape_timer_active = False
            in_post_mb_location = False
            
            try:
                # 1. Check for hyper beam (strongest evidence)
                hyper_beam_data = boss_memory_data.get('beams', b'')
                if len(hyper_beam_data) >= 2:
                    beam_val = struct.unpack('<H', hyper_beam_data[:2])[0]
                    hyper_beam_active = bool(beam_val & 0x1000)  # Hyper beam bit
                
                # 2. Check escape timer (definitive evidence)
                escape_timer_active = any([
                    escape_timer_1_val > 0, escape_timer_2_val > 0,
                    escape_timer_3_val > 0, escape_timer_4_val > 0,
                    escape_timer_5_val > 0, escape_timer_6_val > 0
                ])
                
                # 3. Check location context (supporting evidence)
                area_id = location_data.get('area_id', 0) if location_data else 0
                room_id = location_data.get('room_id', 0) if location_data else 0
                
                # Post-MB locations: Crateria (0), or other areas but NOT in MB room
                in_post_mb_location = (
                    (area_id == 0) or  # Crateria (likely escape sequence)
                    (area_id in [1, 2, 3, 4] and room_id != 56664) or  # Other areas, not MB room
                    (area_id == 5 and room_id != 56664 and room_id > 0)  # Tourian but not MB room
                )
                
                # 4. Additional context: check if we're clearly in a new game
                missiles = location_data.get('missiles', 0) if location_data else 0
                max_missiles = location_data.get('max_missiles', 1) if location_data else 1
                health = location_data.get('current_health', 0) if location_data else 0
                
                # New game indicators (very low progress)
                seems_like_new_game = (
                    area_id in [0, 1] and  # Crateria or Brinstar  
                    room_id < 10000 and    # Early game rooms
                    missiles <= 10 and     # Very few missiles
                    health <= 99           # Starting health
                )
                
            except Exception as e:
                logger.warning(f"Error validating MB2 cache: {e}")
                hyper_beam_active = False
                escape_timer_active = False
                in_post_mb_location = False
                seems_like_new_game = False
            
            # VALIDATE: Cache is only valid if supporting evidence exists
            cache_supporting_evidence = [
                hyper_beam_active,      # Hyper beam still active
                escape_timer_active,    # Escape sequence ongoing  
                in_post_mb_location,    # In post-MB areas
                # Add negation of new game indicators
                not seems_like_new_game
            ]
            
            cache_still_valid = any(cache_supporting_evidence)
            
            logger.info(f"🔍 CACHE VALIDATION: hyper_beam={hyper_beam_active}, escape_timer={escape_timer_active}, post_mb_location={in_post_mb_location}, new_game={seems_like_new_game}")
            
            if cache_still_valid:
                logger.info("✅ MB2 cache VALIDATED - supporting evidence found, keeping cache")
                final_mb2 = True
                final_mb1 = True  # MB2 implies MB1
            else:
                logger.info("❌ MB2 cache INVALID - no supporting evidence, clearing stale cache")
                logger.info("🗑️ CLEARING STALE CACHE: Likely new game, save state reload, or invalid session")
                # Clear both MB1 and MB2 cache
                self.mother_brain_phase_state['mb2_detected'] = False
                self.mother_brain_phase_state['mb1_detected'] = False
                # Use fresh detection only
                final_mb2 = mb2_detected
                final_mb1 = mb1_detected or current_mb1_cache if not seems_like_new_game else mb1_detected
        else:
            # No MB2 cache, use fresh detection
            final_mb2 = mb2_detected
            final_mb1 = mb1_detected or current_mb1_cache
        
        # ESCAPE SEQUENCE SPECIAL CASE: If we're outside MB room and have MB1, strongly suggest MB2
        if not in_mb_room and final_mb1 and not final_mb2:
            # Check if we're in escape-like conditions
            escape_indicators = [
                area_id in [0, 5],  # Crateria or Tourian
                room_id != 56664,   # Not in MB room
                (boss_hp_1_val == 0 and boss_hp_2_val == 0 and boss_hp_3_val == 0)  # No boss HP
            ]
            if any(escape_indicators):
                logger.info(f"🚨 ESCAPE SEQUENCE MB2 INFERENCE: MB1={final_mb1} + escape indicators = MB2=True")
                final_mb2 = True
                self.mother_brain_phase_state['mb2_detected'] = True
        
        # Final boss state assignment
        bosses['mother_brain_1'] = final_mb1
        bosses['mother_brain_2'] = final_mb2
        
        # Log final state for debugging
        logger.info(f"🎯 FINAL MB STATE: MB1={final_mb1}, MB2={final_mb2} (method: {detection_method})")
        
        # End-game detection (Samus reaching her ship)
        samus_ship_detected = self._detect_samus_ship(boss_memory_data, location_data, original_mb1_state, final_mb1, final_mb2)
        bosses['samus_ship'] = samus_ship_detected
        
        # Update previous HP cache for next detection cycle - ensure mb_official_hp is always valid
        try:
            self.previous_mb_hp = mb_official_hp
        except NameError:
            # Safety fallback if mb_official_hp was never set due to an exception
            logger.warning("mb_official_hp was not set, using fallback value 0")
            self.previous_mb_hp = 0
        
        return bosses
    
    def _detect_samus_ship(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any], 
                          main_mb_complete: bool, mb1_complete: bool, mb2_complete: bool) -> bool:
        """Detect when Samus has reached her ship (end-game completion) - hybrid approach"""
        if not location_data:
            logger.debug("Ship detection: No location data")
            return False
            
        area_id = location_data.get('area_id', 0)
        room_id = location_data.get('room_id', 0)
        player_x = location_data.get('player_x', 0)
        player_y = location_data.get('player_y', 0)
        
        # ❗️2. FORCE SHIP DETECTION - Early return if MB2 complete and position valid
        # DISABLED: This was too aggressive and triggered when near ship, not on ship
        # if mb2_complete and player_x > 900 and player_x < 1200 and player_y > 1100 and player_y < 1300:
        #     logger.info(f"🚢 FORCE SHIP DETECTION: MB2 complete and ON SHIP position ({player_x}, {player_y})")
        #     return True
        
        # Debug current state
        logger.info(f"🚢 Ship Debug - Area: {area_id}, Room: {room_id}, Pos: ({player_x},{player_y})")
        logger.info(f"🚢 Ship Debug - MB Status: main={main_mb_complete}, MB1={mb1_complete}, MB2={mb2_complete}")
        
        # Check if Mother Brain sequence is complete
        mother_brain_complete = main_mb_complete or (mb1_complete and mb2_complete)
        partial_mb_complete = mb1_complete  # MB1 completion indicates significant progress
        
        if not mother_brain_complete and not partial_mb_complete:
            logger.info(f"🚢 Ship Debug - No Mother Brain progress")
            return False
        
        # METHOD 1: OFFICIAL AUTOSPLITTER DETECTION (high priority)
        ship_ai_val = 0
        event_flags_val = 0
        
        if boss_memory_data.get('ship_ai') and len(boss_memory_data['ship_ai']) >= 2:
            ship_ai_val = struct.unpack('<H', boss_memory_data['ship_ai'])[0]
        if boss_memory_data.get('event_flags') and len(boss_memory_data['event_flags']) >= 1:
            event_flags_val = struct.unpack('<B', boss_memory_data['event_flags'])[0]
            
        zebes_ablaze = (event_flags_val & 0x40) > 0
        ship_ai_reached = (ship_ai_val == 0xaa4f)
        official_ship_detection = zebes_ablaze and ship_ai_reached
        
        logger.info(f"🚢 OFFICIAL DETECTION - shipAI: 0x{ship_ai_val:04X}, eventFlags: 0x{event_flags_val:02X}")
        logger.info(f"🚢 zebesAblaze: {zebes_ablaze}, shipAI_reached: {ship_ai_reached}")
        
        if official_ship_detection:
            logger.info(f"🚢 ✅ OFFICIAL SHIP DETECTION: Zebes ablaze + shipAI 0xaa4f = SHIP REACHED!")
            return True
        
        # METHOD 2: RELAXED AREA DETECTION
        # Try both traditional Crateria (area 0) AND escape sequence areas
        in_crateria = (area_id == 0)
        in_possible_escape_area = (area_id in [0, 1, 2, 3, 4, 5])  # Be more permissive with areas
        
        logger.info(f"🚢 AREA CHECK - inCrateria: {in_crateria}, inPossibleEscape: {in_possible_escape_area}")
        
        # METHOD 3: EMERGENCY SHIP DETECTION - If MB2 complete + reasonable position data
        # DISABLED: Too aggressive - triggers anywhere in Crateria after MB2
        # This catches cases where area detection fails but user is clearly at ship
        emergency_conditions = (
            mb2_complete and  # MB2 must be complete
            (area_id == 0 or area_id == 5) and  # Common areas during escape/ship sequence  
            (player_x > 1200 and player_y > 1300)  # Must be in very specific ship coordinates
        )
        
        if emergency_conditions:
            logger.info(f"🚢 🚨 EMERGENCY SHIP DETECTION: MB2 complete + valid area/position!")
            return True
        
        # If we have MB2 complete, be VERY permissive with area detection
        # (the area memory might be wrong or the escape sequence uses different areas)
        if mb2_complete:
            logger.info(f"🚢 MB2 COMPLETE - Using relaxed area detection")
            area_check_passed = in_possible_escape_area
        else:
            area_check_passed = in_crateria
            
        if not area_check_passed:
            logger.info(f"🚢 Ship Debug - Area check failed (area={area_id}), ship detection blocked")
            return False
        
        # METHOD 3: POSITION-BASED DETECTION (backup - was working before)
        precise_landing_site_rooms = [31224, 37368]  # Known working rooms
        reasonable_ship_room_ranges = [(31220, 31230), (37360, 37375), (0, 100)]  # Added room 0 range for escape
        
        in_exact_ship_room = room_id in precise_landing_site_rooms
        in_ship_room_range = any(start <= room_id <= end for start, end in reasonable_ship_room_ranges)
        
        # Position-based criteria (RELAXED for escape sequence) 
        ship_exact_x_range = (1150 <= player_x <= 1350)  # Precise ship coordinates
        ship_exact_y_range = (1080 <= player_y <= 1380)  # Extended downward for ship entry
        precise_ship_position = ship_exact_x_range and ship_exact_y_range
        ship_escape_x_range = (1100 <= player_x <= 1400)  # Much more restrictive X range for ship area
        ship_escape_y_range = (1050 <= player_y <= 1400)  # Extended downward for ship area
        broad_ship_position = ship_escape_x_range and ship_escape_y_range
        
        logger.info(f"🚢 POSITION DETECTION - Room: {room_id}, ExactRoom: {in_exact_ship_room}, RangeRoom: {in_ship_room_range}")
        logger.info(f"🚢 POSITION DETECTION - Pos: ({player_x},{player_y}), PrecisePos: {precise_ship_position}, BroadPos: {broad_ship_position}")
        
        # Position-based ship criteria
        exact_position_detection = in_exact_ship_room and precise_ship_position
        escape_sequence_detection = in_ship_room_range and broad_ship_position  # RELAXED: Any reasonable room + broad position
        position_ship_detection = exact_position_detection or escape_sequence_detection
        
        # METHOD 4: EMERGENCY SHIP DETECTION FOR MB2 COMPLETE
        # DISABLED: Extremely aggressive - triggers anywhere after MB2 completion
        # If MB2 is complete and we're in ANY reasonable area/room, assume ship reached
        # if mb2_complete and (room_id > 0 or player_x > 0 or player_y > 0):  # Basic sanity check for valid data
        #     logger.info(f"🚢 🚨 EMERGENCY SHIP DETECTION: MB2 complete + valid position data = SHIP REACHED!")
        #     logger.info(f"🚢 🚨 ACTUAL VALUES - Area:{area_id}, Room:{room_id}, X:{player_x}, Y:{player_y}")
        #     logger.info(f"🚢 🚨 PLEASE RECORD THESE VALUES FOR FUTURE SHIP DETECTION!")
        #     return True
        
        if position_ship_detection:
            detection_type = "precise" if exact_position_detection else "escape_sequence"
            logger.info(f"🚢 ✅ POSITION SHIP DETECTION ({detection_type}): MB complete + area OK + correct room + ship position!")
            return True
        
        logger.info(f"🚢 ❌ Ship not detected by any method")
        return False
    
    def maybe_reset_mb_state(self, location_data: Dict[str, Any], stats_data: Optional[bytes]):
        """Reset MB phase tracking on game start, save load, or contradiction detection"""
        if not location_data:
            return
            
        area_id = location_data.get('area_id', -1)
        room_id = location_data.get('room_id', -1)
        missiles = location_data.get('missiles', 0)
        max_missiles = location_data.get('max_missiles', 0)
        
        # Get health to detect new games (low health = likely new save)
        health = 0
        if stats_data and len(stats_data) >= 2:
            health = struct.unpack('<H', stats_data[0:2])[0]
        
        # SHIP DETECTION SAFETY - Don't reset MB state if conditions suggest ship detection should happen
        # This prevents false resets when user finishes the game
        mb1_cached = self.mother_brain_phase_state.get('mb1_detected', False)
        mb2_cached = self.mother_brain_phase_state.get('mb2_detected', False)
        
        # If MB2 is complete, be VERY conservative about resets - user might be finishing game
        ship_completion_likely = (
            mb2_cached and  # MB2 already completed
            (area_id in [0, 1, 2, 3, 4, 5]) and  # In any reasonable area (escape sequence)
            health > 50 and  # Not dead
            missiles >= 0    # Basic sanity check
        )
        
        if ship_completion_likely:
            logger.info(f"🚢 SHIP COMPLETION PROTECTION: MB2 complete, preserving cache during suspected game completion")
            logger.info(f"🚢 Area:{area_id}, Room:{room_id}, Health:{health}, Missiles:{missiles}")
            return  # Skip all reset logic
        
        # Reset conditions (but now with ship completion protection):
        
        # 1. New game detection: Crateria + low room ID + low health + no significant progress
        new_game_detected = (
            area_id == 0 and 
            room_id < 32000 and 
            health <= 99
            # REMOVED: and not (mb1_cached or mb2_cached)
            # If we detect new game conditions, ALWAYS reset cache regardless of current state
        )
        
        # 2. Save state contradiction: ONLY in specific pre-fight scenarios
        # Much more conservative - only reset if clearly in starting area with full missiles
        save_contradiction = (
            (mb1_cached or mb2_cached) and
            area_id == 0 and  # In Crateria (starting area)
            room_id < 35000 and  # In early Crateria rooms (not ship area)
            missiles > 0 and max_missiles > 0 and
            missiles >= (max_missiles * 0.9) and  # Nearly full missiles
            health > 400  # High health (pre-fight state)
        )
        
        # 3. Reset detection: VERY conservative - only in clearly pre-game areas
        early_area_reset = (
            (mb1_cached or mb2_cached) and
            area_id in [0, 1] and  # Only Crateria/Brinstar (very early areas)
            room_id < 30000 and  # Early room IDs only
            health > 400 and  # High health (not post-MB state)
            missiles > 150    # Very high missile count (clearly pre-fight)
        )
        
        # 4. Memory contradiction: ONLY if in MB room with impossible state
        memory_contradiction = (
            area_id == 5 and room_id == 56664 and  # In MB room
            missiles > 0 and max_missiles > 0 and
            missiles >= (max_missiles * 0.95) and  # Nearly full missiles
            health > 600  # Very high health (impossible after MB fight)
        )
        
        # 5. REMOVED general_contradiction - too aggressive for end-game scenarios
        
        # Only reset if we have clear evidence of save state reload to pre-fight state
        should_reset = (new_game_detected or save_contradiction or early_area_reset or memory_contradiction)
        
        if should_reset:
            reason = ("new game" if new_game_detected else 
                     "save state load (early Crateria)" if save_contradiction else
                     "game reset (early area)" if early_area_reset else
                     "impossible MB room state" if memory_contradiction else
                     "unknown")
            logger.info(f"🔄 Resetting MB state cache - detected {reason}")
            logger.info(f"   └── Area: {area_id}, Room: {room_id}, Health: {health}, Missiles: {missiles}/{max_missiles}")
            self.mother_brain_phase_state['mb1_detected'] = False
            self.mother_brain_phase_state['mb2_detected'] = False
        else:
            # Log why we're NOT resetting (helpful for debugging)
            if mb1_cached or mb2_cached:
                logger.info(f"🔒 PRESERVING MB CACHE: No clear reset conditions met")
                logger.info(f"   └── Area: {area_id}, Room: {room_id}, Health: {health}, Missiles: {missiles}/{max_missiles}")
    
    def reset_mb_cache(self):
        """Manually reset Mother Brain phase cache (for testing)"""
        self.mother_brain_phase_state['mb1_detected'] = False
        self.mother_brain_phase_state['mb2_detected'] = False
    
    def bootstrap_mb_cache(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any] = None):
        """Bootstrap MB cache by checking current state - useful after implementing persistent state"""
        logger.info("🔄 Bootstrapping MB cache from current game state...")
        
        # Temporarily disable cache to get raw detection
        old_mb1_state = self.mother_brain_phase_state['mb1_detected']
        old_mb2_state = self.mother_brain_phase_state['mb2_detected']
        
        # Reset to get clean detection
        self.mother_brain_phase_state['mb1_detected'] = False
        self.mother_brain_phase_state['mb2_detected'] = False
        
        # Parse current state without cache influence
        current_bosses = self.parse_bosses(boss_memory_data, location_data)
        
        # If MB phases are detected now, cache them
        if current_bosses.get('mother_brain_1', False):
            self.mother_brain_phase_state['mb1_detected'] = True
            logger.info("🎯 Bootstrapped MB1 state: detected and cached")
        else:
            self.mother_brain_phase_state['mb1_detected'] = old_mb1_state
            
        if current_bosses.get('mother_brain_2', False):
            self.mother_brain_phase_state['mb2_detected'] = True
            logger.info("🎯 Bootstrapped MB2 state: detected and cached")
        else:
            self.mother_brain_phase_state['mb2_detected'] = old_mb2_state
            
        logger.info(f"🔄 Bootstrap complete: MB1={self.mother_brain_phase_state['mb1_detected']}, MB2={self.mother_brain_phase_state['mb2_detected']}")
        
        return current_bosses
    
    def parse_complete_game_state(self, memory_data: Dict[str, bytes]) -> Dict[str, Any]:
        """Parse all memory data into complete game state"""
        try:
            game_state = {}
            
            # Location data first (needed for intro scene detection)
            location_data = self.parse_location_data(
                memory_data.get('room_id'),
                memory_data.get('area_id'), 
                memory_data.get('game_state'),
                memory_data.get('player_x'),
                memory_data.get('player_y')
            )
            game_state.update(location_data)
            
            # Basic stats (with intro scene detection)
            stats_data = memory_data.get('basic_stats')
            if stats_data:
                game_state.update(self.parse_basic_stats(stats_data, location_data))
            
            # Optional: reset if new game or file load
            self.maybe_reset_mb_state(location_data, stats_data)
            
            # Items and beams
            game_state['items'] = self.parse_items(memory_data.get('items'), location_data, game_state.get('health', 0))
            game_state['beams'] = self.parse_beams(memory_data.get('beams'), location_data, game_state.get('health', 0))
            
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