#!/usr/bin/env python3
"""
Unified Super Metroid Tracker Server
Single process that handles both UDP tracking and web serving
SIMPLIFIED: No more separate processes or complex state management
"""

import json
import socket
import struct
import time
import logging
import sys
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Dict, Optional

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('unified_tracker.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class UnifiedSuperMetroidTracker:
    """Single class that handles both UDP tracking and serves web interface"""
    
    def __init__(self, retroarch_host="localhost", retroarch_port=55355):
        self.retroarch_host = retroarch_host
        self.retroarch_port = retroarch_port
        self.udp_sock = None
        self.last_stats = {}
        self.last_successful_read = 0
        
        # Super Metroid memory addresses
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
            'game_state': 0x7E0998,
            'room_id': 0x7E079B,
            'area_id': 0x7E079F,
            'player_x': 0x7E0AF6,
            'player_y': 0x7E0AFA,
            'items_collected': 0x7E09A4,
            'beams_collected': 0x7E09A8,
            'bosses_defeated': 0x7ED828,
            'crocomire_defeated': 0x7ED829,
            'events_flags': 0x7ED870,
        }
        
        self.areas = {
            0: "Crateria", 1: "Brinstar", 2: "Norfair",
            3: "Wrecked Ship", 4: "Maridia", 5: "Tourian"
        }
        
    def connect_udp(self) -> bool:
        """Connect to RetroArch via UDP"""
        try:
            if self.udp_sock:
                self.udp_sock.close()
            self.udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.udp_sock.settimeout(2.0)
            return True
        except Exception as e:
            logger.error(f"Failed to create UDP socket: {e}")
            return False
            
    def send_retroarch_command(self, command: str) -> Optional[str]:
        """Send command to RetroArch"""
        max_retries = 2
        
        for attempt in range(max_retries):
            if not self.udp_sock:
                if not self.connect_udp():
                    return None
                    
            try:
                # Clear any pending data first
                self.udp_sock.settimeout(0.1)
                try:
                    while True:
                        self.udp_sock.recv(1024)
                except socket.timeout:
                    pass
                
                # Set normal timeout and send command
                self.udp_sock.settimeout(2.0)
                self.udp_sock.sendto(command.encode(), (self.retroarch_host, self.retroarch_port))
                data, addr = self.udp_sock.recvfrom(1024)
                response = data.decode().strip()
                
                # Validate response makes sense for command
                if command == "VERSION" and response.startswith("GET_STATUS"):
                    logger.warning(f"Got GET_STATUS response for VERSION command, retrying...")
                    self.connect_udp()  # Fresh socket
                    continue
                elif command == "GET_STATUS" and not response.startswith("GET_STATUS") and len(response) < 10:
                    logger.warning(f"Got short response for GET_STATUS command, retrying...")
                    self.connect_udp()  # Fresh socket
                    continue
                    
                return response
                
            except socket.timeout:
                logger.warning(f"RetroArch command timeout: {command} (attempt {attempt + 1})")
                self.connect_udp()  # Fresh socket for retry
            except Exception as e:
                logger.error(f"RetroArch command error: {e} (attempt {attempt + 1})")
                self.connect_udp()  # Fresh socket for retry
                
        return None
            
    def is_retroarch_connected(self) -> bool:
        """Check if RetroArch is responding"""
        response = self.send_retroarch_command("VERSION")
        return response is not None
        
    def is_game_loaded(self) -> bool:
        """Check if Super Metroid is loaded and playing"""
        response = self.send_retroarch_command("GET_STATUS")
        if response and "PLAYING" in response:
            response_lower = response.lower()
            if "super metroid" in response_lower:
                return True
            # Check for ROM hacks and randomizers
            rom_hack_keywords = [
                "metroid", "samus", "zebes", "crateria", "norfair", "maridia",
                "map rando", "rando", "randomizer", "random", "sm ", "super_metroid"
            ]
            return any(keyword in response_lower for keyword in rom_hack_keywords)
        return False
        
    def read_memory_range(self, start_address: int, size: int) -> Optional[bytes]:
        """Read memory range from RetroArch"""
        command = f"READ_CORE_MEMORY 0x{start_address:X} {size}"
        response = self.send_retroarch_command(command)
        
        if not response or not response.startswith("READ_CORE_MEMORY"):
            return None
            
        try:
            parts = response.split(' ', 2)
            if len(parts) < 3:
                return None
            hex_data = parts[2].replace(' ', '')
            return bytes.fromhex(hex_data)
        except ValueError:
            return None
            
    def read_word(self, address: int) -> Optional[int]:
        """Read 16-bit word from memory"""
        data = self.read_memory_range(address, 2)
        if data and len(data) >= 2:
            return struct.unpack('<H', data[:2])[0]
        return None
        
    def read_byte(self, address: int) -> Optional[int]:
        """Read single byte from memory"""
        data = self.read_memory_range(address, 1)
        if data and len(data) >= 1:
            return data[0]
        return None
        
    def get_game_stats(self) -> Dict:
        """Read all Super Metroid stats"""
        try:
            # Read core stats
            base_address = self.memory_map['health']
            data = self.read_memory_range(base_address, 22)
            
            if not data or len(data) < 22:
                return {}
                
            stats = {
                'health': struct.unpack('<H', data[0:2])[0],
                'max_health': struct.unpack('<H', data[2:4])[0],
                'missiles': struct.unpack('<H', data[4:6])[0],
                'max_missiles': struct.unpack('<H', data[6:8])[0],
                'supers': struct.unpack('<H', data[8:10])[0],
                'max_supers': struct.unpack('<H', data[10:12])[0],
                'power_bombs': struct.unpack('<H', data[12:14])[0],
                'max_power_bombs': struct.unpack('<H', data[14:16])[0],
                'max_reserve_energy': struct.unpack('<H', data[18:20])[0],
                'reserve_energy': struct.unpack('<H', data[20:22])[0],
            }
            
            # Read location data
            stats['room_id'] = self.read_word(self.memory_map['room_id']) or 0
            stats['area_id'] = self.read_byte(self.memory_map['area_id']) or 0
            stats['area_name'] = self.areas.get(stats['area_id'], "Unknown")
            stats['game_state'] = self.read_word(self.memory_map['game_state']) or 0
            stats['player_x'] = self.read_word(self.memory_map['player_x']) or 0
            stats['player_y'] = self.read_word(self.memory_map['player_y']) or 0
            
            # Read item and boss data
            items_data = self.read_word(self.memory_map['items_collected'])
            beams_data = self.read_word(self.memory_map['beams_collected'])
            bosses_data = self.read_word(self.memory_map['bosses_defeated'])
            
            # Parse items
            stats['items'] = {}
            if items_data is not None:
                stats['items'] = {
                    'morph': bool(items_data & 0x04),
                    'bombs': bool(items_data & 0x1000),
                    'varia': bool(items_data & 0x01),
                    'gravity': bool(items_data & 0x20),
                    'hijump': bool(items_data & 0x100),
                    'speed': bool(items_data & 0x2000),
                    'space': bool(items_data & 0x200),
                    'screw': bool(items_data & 0x08),
                    'spring': bool(items_data & 0x02),
                    'xray': bool(items_data & 0x8000),
                    'grapple': bool(items_data & 0x4000),
                }
                
            # Parse beams
            stats['beams'] = {}
            if beams_data is not None:
                stats['beams'] = {
                    'charge': bool(beams_data & 0x1000),
                    'ice': bool(beams_data & 0x02),
                    'wave': bool(beams_data & 0x01),
                    'spazer': bool(beams_data & 0x04),
                    'plasma': bool(beams_data & 0x08),
                }
                
            # Parse bosses with our fixed logic
            stats['bosses'] = {}
            if bosses_data is not None:
                crocomire_data = self.read_word(self.memory_map['crocomire_defeated'])
                
                # Boss scan addresses
                boss_scan_results = {}
                scan_addresses = {
                    'boss_plus_1': 0x7ED829, 'boss_plus_2': 0x7ED82A,
                    'boss_plus_3': 0x7ED82B, 'boss_plus_4': 0x7ED82C,
                    'boss_plus_5': 0x7ED82D, 'boss_minus_1': 0x7ED827,
                }
                
                for scan_name, addr in scan_addresses.items():
                    scan_data = self.read_word(addr)
                    if scan_data is not None:
                        boss_scan_results[scan_name] = scan_data
                
                # Fixed boss detection logic
                phantoon_addr = boss_scan_results.get('boss_plus_3', 0)
                phantoon_detected = phantoon_addr and (phantoon_addr & 0x01) and (phantoon_addr & 0x0300)
                
                botwoon_addr_1 = boss_scan_results.get('boss_plus_2', 0)
                botwoon_addr_2 = boss_scan_results.get('boss_plus_4', 0)
                botwoon_detected = ((botwoon_addr_1 & 0x04) and (botwoon_addr_1 > 0x0100)) or \
                                 ((botwoon_addr_2 & 0x02) and (botwoon_addr_2 > 0x0001))
                
                space_jump = stats['items'].get('space', False)
                draygon_detected = False
                if space_jump:
                    for scan_value in boss_scan_results.values():
                        if scan_value is not None and (scan_value & 0x04):
                            draygon_detected = True
                            break
                
                ridley_detected = False
                ridley_candidates = [
                    ('boss_plus_1', 0x400, 0x0400),
                    ('boss_plus_1', 0x200, 0x0A00),
                    ('boss_plus_1', 0x100, 0x0500),
                    ('boss_plus_2', 0x100, 0x0100),
                ]
                
                for scan_name, bit_mask, min_value in ridley_candidates:
                    candidate_data = boss_scan_results.get(scan_name, 0)
                    if (candidate_data & bit_mask) and (candidate_data >= min_value):
                        ridley_detected = True
                        break
                
                boss_plus_1_val = boss_scan_results.get('boss_plus_1', 0)
                boss_plus_2_val = boss_scan_results.get('boss_plus_2', 0)
                boss_plus_3_val = boss_scan_results.get('boss_plus_3', 0)
                
                condition1 = ((boss_plus_1_val & 0x0700) and (boss_plus_1_val & 0x0003) and (boss_plus_1_val >= 0x0703))
                condition2 = (boss_plus_2_val & 0x0100) and (boss_plus_2_val >= 0x0500)
                condition3 = (boss_plus_3_val & 0x0300) and (boss_plus_3_val >= 0x0300)
                golden_torizo_detected = condition1 or condition2 or condition3
                
                stats['bosses'] = {
                    'bomb_torizo': bool(bosses_data & 0x04),
                    'kraid': bool(bosses_data & 0x100),
                    'spore_spawn': bool(bosses_data & 0x200),
                    'crocomire': bool(crocomire_data & 0x02) and (crocomire_data >= 0x0202) if crocomire_data is not None else False,
                    'phantoon': phantoon_detected,
                    'botwoon': botwoon_detected,
                    'draygon': draygon_detected,
                    'ridley': ridley_detected,
                    'golden_torizo': golden_torizo_detected,
                    'mother_brain': bool(bosses_data & 0x01),
                }
            
            self.last_stats = stats
            self.last_successful_read = time.time()
            return stats
            
        except Exception as e:
            logger.error(f"Error reading game stats: {e}")
            return {}
    
    def get_full_status(self) -> Dict:
        """Get complete tracker status"""
        status = {
            'connected': False,
            'game_loaded': False,
            'retroarch_version': None,
            'game_info': None,
            'stats': {},
            'last_update': None
        }
        
        try:
            # Ensure fresh connection for each status check
            if not self.connect_udp():
                return status
                
            # Check RetroArch connection with VERSION command
            version = self.send_retroarch_command("VERSION")
            if not version or version.startswith("GET_STATUS"):
                # If we get confused response, reconnect and try again
                self.connect_udp()
                version = self.send_retroarch_command("VERSION")
                
            if version and not version.startswith("GET_STATUS"):
                status['connected'] = True
                status['retroarch_version'] = version
                
                # Check if game is loaded with separate socket call
                game_loaded = self.is_game_loaded()
                if game_loaded:
                    status['game_loaded'] = True
                    status['game_info'] = self.send_retroarch_command("GET_STATUS")
                    
                    # Get game stats
                    stats = self.get_game_stats()
                    if stats:
                        status['stats'] = stats
                        status['last_update'] = time.time()
                    elif self.last_stats and (time.time() - self.last_successful_read < 10):
                        # Use cached stats if recent
                        status['stats'] = self.last_stats
                        status['last_update'] = self.last_successful_read
            else:
                logger.warning(f"Unexpected VERSION response: {repr(version)}")
                
        except Exception as e:
            logger.error(f"Error in get_full_status: {e}")
        
        return status

class UnifiedTrackerHandler(BaseHTTPRequestHandler):
    """HTTP handler that uses the unified tracker"""
    
    def __init__(self, *args, tracker=None, **kwargs):
        self.tracker = tracker
        super().__init__(*args, **kwargs)
        
    def do_GET(self):
        """Handle GET requests"""
        try:
            if self.path == '/api/status':
                self.serve_status()
            elif self.path == '/api/stats':
                self.serve_stats()
            elif self.path == '/':
                self.serve_file('super_metroid_tracker.html')
            elif self.path.endswith('.html'):
                self.serve_file(self.path[1:])
            elif self.path.endswith('.png'):
                self.serve_static_file(self.path[1:], 'image/png')
            elif self.path.endswith('.css'):
                self.serve_static_file(self.path[1:], 'text/css')
            elif self.path.endswith('.js'):
                self.serve_static_file(self.path[1:], 'application/javascript')
            else:
                self.send_error(404)
        except Exception as e:
            logger.error(f"Request error: {e}")
            self.send_error(500)
            
    def do_OPTIONS(self):
        """Handle CORS preflight"""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        
    def serve_status(self):
        """Serve tracker status"""
        try:
            status = self.tracker.get_full_status()
            self.send_json_response(status)
        except Exception as e:
            logger.error(f"Status error: {e}")
            self.send_json_response({'error': str(e)}, 500)
            
    def serve_stats(self):
        """Serve just stats"""
        try:
            status = self.tracker.get_full_status()
            stats = status.get('stats', {})
            if not stats:
                stats = {'error': 'No game data available'}
            self.send_json_response(stats)
        except Exception as e:
            logger.error(f"Stats error: {e}")
            self.send_json_response({'error': str(e)}, 500)
            
    def serve_file(self, filename):
        """Serve HTML files"""
        try:
            with open(filename, 'r') as f:
                content = f.read()
            self.send_response(200)
            self.send_header('Content-Type', 'text/html')
            self.send_header('Content-Length', len(content.encode()))
            self.end_headers()
            self.wfile.write(content.encode())
        except FileNotFoundError:
            self.send_error(404)
        except Exception as e:
            logger.error(f"File serve error: {e}")
            self.send_error(500)
            
    def serve_static_file(self, filename, content_type):
        """Serve static files"""
        try:
            with open(filename, 'rb') as f:
                content = f.read()
            self.send_response(200)
            self.send_header('Content-Type', content_type)
            self.send_header('Content-Length', len(content))
            self.end_headers()
            self.wfile.write(content)
        except FileNotFoundError:
            self.send_error(404)
        except Exception as e:
            logger.error(f"Static file error: {e}")
            self.send_error(500)
            
    def send_json_response(self, data, status_code=200):
        """Send JSON response"""
        try:
            json_data = json.dumps(data, indent=2)
            self.send_response(status_code)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Content-Length', len(json_data.encode()))
            self.end_headers()
            self.wfile.write(json_data.encode())
        except Exception as e:
            logger.error(f"JSON response error: {e}")
            
    def log_message(self, format, *args):
        """Suppress HTTP request logging"""
        pass

class UnifiedTrackerServer:
    """Single unified server"""
    
    def __init__(self, port=3000):
        self.port = port
        self.tracker = UnifiedSuperMetroidTracker()
        self.server = None
        
    def start(self):
        """Start the unified server"""
        logger.info("🚀 Starting Unified Super Metroid Tracker Server...")
        
        def handler(*args, **kwargs):
            UnifiedTrackerHandler(*args, tracker=self.tracker, **kwargs)
            
        try:
            self.server = HTTPServer(('localhost', self.port), handler)
            
            logger.info("✅ Unified server started successfully:")
            logger.info(f"   📱 Tracker UI: http://localhost:{self.port}/")
            logger.info(f"   📊 API Status: http://localhost:{self.port}/api/status")
            logger.info(f"   📈 API Stats:  http://localhost:{self.port}/api/stats")
            logger.info(f"   🎯 Single process - no complexity!")
            logger.info(f"   ⏹️  Press Ctrl+C to stop")
            
            self.server.serve_forever()
        except KeyboardInterrupt:
            logger.info("⚠️ Server stopped by user")
        except Exception as e:
            logger.error(f"❌ Server error: {e}")
        finally:
            self.stop()
            
    def stop(self):
        """Stop the server"""
        if self.server:
            self.server.shutdown()
            self.server.server_close()
        if self.tracker.udp_sock:
            self.tracker.udp_sock.close()

def main():
    """Start the unified server with auto-restart"""
    max_restarts = 3
    restart_count = 0
    
    while restart_count < max_restarts:
        try:
            logger.info(f"🚀 Starting unified server (attempt {restart_count + 1})")
            server = UnifiedTrackerServer(port=3000)
            server.start()
            break
        except KeyboardInterrupt:
            logger.info("👋 Goodbye!")
            break
        except Exception as e:
            restart_count += 1
            logger.error(f"❌ Server crashed: {e}")
            if restart_count < max_restarts:
                wait_time = 5 * restart_count
                logger.info(f"🔄 Restarting in {wait_time} seconds...")
                time.sleep(wait_time)
            else:
                logger.error("❌ Max restarts reached.")

if __name__ == "__main__":
    main() 