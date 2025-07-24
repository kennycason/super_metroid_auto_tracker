#!/usr/bin/env python3
"""
Background Polling Super Metroid Tracker Server

NEW ARCHITECTURE:
- Background UDP poller runs every 2-3 seconds
- HTTP server serves cached data instantly  
- No blocking, no request flooding, much more stable

Usage: python background_poller_server.py
"""

import json
import socket
import struct
import time
import logging
import threading
import queue
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Dict, Any, Optional
import sys
import signal

from game_state_parser import SuperMetroidGameStateParser

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('background_poller.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class RetroArchUDPReader:
    """Handles UDP communication with RetroArch - separated from parsing logic"""
    
    def __init__(self, host="localhost", port=55355):
        self.host = host
        self.port = port
        self.sock = None
        self.last_connection_attempt = 0
        self.connection_retry_delay = 5  # seconds
        
    def connect(self) -> bool:
        """Connect to RetroArch UDP interface"""
        try:
            if self.sock:
                self.sock.close()
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.sock.settimeout(1.0)  # Fast timeout for background polling
            return True
        except Exception as e:
            logger.error(f"UDP connection failed: {e}")
            return False
    
    def send_command(self, command: str) -> Optional[str]:
        """Send single command to RetroArch"""
        if not self.sock:
            if not self.connect():
                return None
                
        try:
            # Clear any pending data
            self.sock.settimeout(0.1)
            try:
                while True:
                    self.sock.recv(1024)
            except socket.timeout:
                pass
                
            # Send command
            self.sock.settimeout(1.0)
            self.sock.sendto(command.encode(), (self.host, self.port))
            data, addr = self.sock.recvfrom(1024)
            return data.decode().strip()
            
        except socket.timeout:
            logger.debug(f"UDP timeout for command: {command}")
            return None
        except Exception as e:
            logger.debug(f"UDP error for command {command}: {e}")
            return None
    
    def read_memory_range(self, start_address: int, size: int) -> Optional[bytes]:
        """Read memory range from RetroArch"""
        command = f"READ_CORE_MEMORY 0x{start_address:X} {size}"
        response = self.send_command(command)
        
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
    
    def is_game_loaded(self) -> bool:
        """Check if Super Metroid is loaded"""
        response = self.send_command("GET_STATUS")
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
    
    def get_retroarch_info(self) -> Dict[str, Any]:
        """Get RetroArch connection info"""
        version = self.send_command("VERSION")
        status = self.send_command("GET_STATUS")
        
        return {
            'connected': version is not None,
            'retroarch_version': version,
            'game_info': status,
            'game_loaded': self.is_game_loaded()
        }

class BackgroundGamePoller:
    """Background thread that polls game state and updates cache"""
    
    def __init__(self, update_interval=2.5):
        self.update_interval = update_interval
        self.udp_reader = RetroArchUDPReader()
        self.parser = SuperMetroidGameStateParser()
        self.cache = {
            'game_state': {},
            'connection_info': {},
            'last_update': 0,
            'poll_count': 0,
            'error_count': 0
        }
        self.cache_lock = threading.Lock()
        self.running = False
        self.thread = None
        self.bootstrap_attempted = False  # Track if we've tried bootstrapping MB cache
        
    def start(self):
        """Start background polling"""
        if self.running:
            return
            
        self.running = True
        self.thread = threading.Thread(target=self._poll_loop, daemon=True)
        self.thread.start()
        logger.info(f"🚀 Background poller started (interval: {self.update_interval}s)")
    
    def stop(self):
        """Stop background polling"""
        self.running = False
        if self.thread:
            self.thread.join(timeout=5)
        logger.info("🛑 Background poller stopped")
    
    def get_cached_state(self) -> Dict[str, Any]:
        """Get current cached game state"""
        with self.cache_lock:
            return {
                'connected': self.cache['connection_info'].get('connected', False),
                'game_loaded': self.cache['connection_info'].get('game_loaded', False),
                'retroarch_version': self.cache['connection_info'].get('retroarch_version'),
                'game_info': self.cache['connection_info'].get('game_info'),
                'stats': self.cache['game_state'],
                'last_update': self.cache['last_update'],
                'poll_count': self.cache['poll_count'],
                'error_count': self.cache['error_count']
            }
    
    def _poll_loop(self):
        """Main polling loop - runs in background thread"""
        logger.info("📡 Starting background polling loop...")
        
        while self.running:
            try:
                start_time = time.time()
                
                # Get connection info
                connection_info = self.udp_reader.get_retroarch_info()
                
                # Read game state if game is loaded
                game_state = {}
                if connection_info.get('game_loaded', False):
                    game_state = self._read_game_state()
                    
                    # Bootstrap MB cache on first successful game read (if we haven't already)
                    if game_state and not self.bootstrap_attempted:
                        self._bootstrap_mb_cache_if_needed(game_state)
                        self.bootstrap_attempted = True
                
                # Update cache atomically
                with self.cache_lock:
                    self.cache['connection_info'] = connection_info
                    if game_state:  # Only update if we got valid data
                        self.cache['game_state'] = game_state
                    self.cache['last_update'] = time.time()
                    self.cache['poll_count'] += 1
                
                poll_duration = time.time() - start_time
                logger.debug(f"Poll completed in {poll_duration:.2f}s")
                
                # Sleep until next poll
                time.sleep(max(0, self.update_interval - poll_duration))
                
            except Exception as e:
                logger.error(f"Polling error: {e}")
                with self.cache_lock:
                    self.cache['error_count'] += 1
                time.sleep(1)  # Brief pause on error
    
    def _read_game_state(self) -> Dict[str, Any]:
        """Read complete game state via bulk memory operations"""
        try:
            # BULK READ: Get all basic stats in one 22-byte read
            basic_stats = self.udp_reader.read_memory_range(0x7E09C2, 22)
            
            # Individual reads for other data
            room_id = self.udp_reader.read_memory_range(0x7E079B, 2)
            area_id = self.udp_reader.read_memory_range(0x7E079F, 1)  # FIXED: Use standard area address (0x7E079F)
            game_state = self.udp_reader.read_memory_range(0x7E0998, 2)
            player_x = self.udp_reader.read_memory_range(0x7E0AF6, 2)
            player_y = self.udp_reader.read_memory_range(0x7E0AFA, 2)
            items = self.udp_reader.read_memory_range(0x7E09A4, 2)
            beams = self.udp_reader.read_memory_range(0x7E09A8, 2)
            
            # Boss memory (multiple addresses for advanced detection)
            main_bosses = self.udp_reader.read_memory_range(0x7ED828, 2)
            crocomire = self.udp_reader.read_memory_range(0x7ED829, 2)
            boss_plus_1 = self.udp_reader.read_memory_range(0x7ED829, 2)  # Fixed: was 0x7ED82A
            boss_plus_2 = self.udp_reader.read_memory_range(0x7ED82A, 2)  # Fixed: was 0x7ED82B
            boss_plus_3 = self.udp_reader.read_memory_range(0x7ED82B, 2)  # Fixed: was 0x7ED82C
            boss_plus_4 = self.udp_reader.read_memory_range(0x7ED82C, 2)  # Added
            boss_plus_5 = self.udp_reader.read_memory_range(0x7ED82D, 2)
            
            # Escape timer for MB2 detection (multiple addresses to try)
            escape_timer_1 = self.udp_reader.read_memory_range(0x7E0943, 2)  # Common escape timer location
            escape_timer_2 = self.udp_reader.read_memory_range(0x7E0945, 2)  # Alternative location
            escape_timer_3 = self.udp_reader.read_memory_range(0x7E09E2, 2)  # Another possible location
            escape_timer_4 = self.udp_reader.read_memory_range(0x7E09E0, 2)  # Another possible location
            escape_timer_5 = self.udp_reader.read_memory_range(0x7E0947, 2)  # Sequential check
            escape_timer_6 = self.udp_reader.read_memory_range(0x7E0949, 2)  # Sequential check
            
            # ADDITIONAL ESCAPE TIMER ADDRESSES - commonly known locations
            escape_timer_7 = self.udp_reader.read_memory_range(0x7E0911, 2)  # Known timer location
            escape_timer_8 = self.udp_reader.read_memory_range(0x7E0913, 2)  # Alternative timer  
            escape_timer_9 = self.udp_reader.read_memory_range(0x7E0915, 2)  # Sequential
            escape_timer_10 = self.udp_reader.read_memory_range(0x7E0917, 2) # Sequential
            escape_timer_11 = self.udp_reader.read_memory_range(0x7E0919, 2) # Sequential
            escape_timer_12 = self.udp_reader.read_memory_range(0x7E0921, 2) # Different block
            
            # MEMORY SCAN - Look for any non-zero timers in common areas
            scan_090x = self.udp_reader.read_memory_range(0x7E0900, 32)  # Scan 0x900-0x91F
            scan_094x = self.udp_reader.read_memory_range(0x7E0940, 32)  # Scan 0x940-0x95F  
            scan_09Ex = self.udp_reader.read_memory_range(0x7E09E0, 32)  # Scan 0x9E0-0x9FF
            
            # Boss HP for direct detection (MB room boss HP)
            boss_hp_1 = self.udp_reader.read_memory_range(0x7E0F8C, 2)  # Common boss HP location
            boss_hp_2 = self.udp_reader.read_memory_range(0x7E0F8E, 2)  # Alternative boss HP
            boss_hp_3 = self.udp_reader.read_memory_range(0x7E1000, 2)  # Another potential location
            
            # OFFICIAL AUTOSPLITTER ADDRESS: Mother Brain HP for phase detection
            mother_brain_official_hp = self.udp_reader.read_memory_range(0x7E0FCC, 2)
            
            # OFFICIAL AUTOSPLITTER ADDRESSES: Ship detection
            ship_ai = self.udp_reader.read_memory_range(0x7E0FB2, 2)    # Ship AI state
            event_flags = self.udp_reader.read_memory_range(0x7ED821, 1) # Event flags (zebesAblaze)
            
            # Game state (escape sequence often changes game state)
            game_state_extended = self.udp_reader.read_memory_range(0x7E0998, 2)
            
            # Package all memory data
            memory_data = {
                'basic_stats': basic_stats,
                'room_id': room_id,
                'area_id': area_id,
                'game_state': game_state,
                'player_x': player_x,
                'player_y': player_y,
                'items': items,
                'beams': beams,
                'main_bosses': main_bosses,
                'crocomire': crocomire,
                'boss_plus_1': boss_plus_1,
                'boss_plus_2': boss_plus_2,
                'boss_plus_3': boss_plus_3,
                'boss_plus_4': boss_plus_4,
                'boss_plus_5': boss_plus_5,
                'escape_timer_1': escape_timer_1,
                'escape_timer_2': escape_timer_2,
                'escape_timer_3': escape_timer_3,
                'escape_timer_4': escape_timer_4,
                'escape_timer_5': escape_timer_5,
                'escape_timer_6': escape_timer_6,
                'escape_timer_7': escape_timer_7,
                'escape_timer_8': escape_timer_8,
                'escape_timer_9': escape_timer_9,
                'escape_timer_10': escape_timer_10,
                'escape_timer_11': escape_timer_11,
                'escape_timer_12': escape_timer_12,
                'scan_090x': scan_090x,
                'scan_094x': scan_094x,
                'scan_09Ex': scan_09Ex,
                'boss_hp_1': boss_hp_1,
                'boss_hp_2': boss_hp_2,
                'boss_hp_3': boss_hp_3,
                'mother_brain_official_hp': mother_brain_official_hp,
                
                # Official autosplitter ship detection
                'ship_ai': ship_ai,
                'event_flags': event_flags,
                'game_state_extended': game_state_extended,
            }
            
            # Parse into structured game state
            parsed_state = self.parser.parse_complete_game_state(memory_data)
            
            if self.parser.is_valid_game_state(parsed_state):
                return parsed_state
            else:
                logger.warning("Invalid game state parsed")
                return {}
                
        except Exception as e:
            logger.error(f"Error reading game state: {e}")
            return {}
    
    def _bootstrap_mb_cache_if_needed(self, game_state: Dict[str, Any]):
        """Bootstrap Mother Brain cache if current state shows MB phases completed"""
        try:
            # Check if current game state shows any MB progress
            bosses = game_state.get('bosses', {})
            mb1_current = bosses.get('mother_brain_1', False)
            mb2_current = bosses.get('mother_brain_2', False)
            
            # CONSERVATIVE BOOTSTRAP: Only bootstrap if we're clearly NOT in a new game
            # Check for new game indicators that would suggest cache should be False
            current_health = game_state.get('current_health', 0)
            max_health = game_state.get('max_health', 0)
            current_missiles = game_state.get('current_missiles', 0)
            max_missiles = game_state.get('max_missiles', 0)
            area_id = game_state.get('area_id', 0)
            room_id = game_state.get('room_id', 0)
            
            # If this looks like a new save file, DON'T bootstrap cache
            new_save_indicators = (
                current_health <= 99 and max_health <= 99 and  # Starting health
                current_missiles <= 10 and max_missiles <= 100  # Low missile count
            )
            
            if new_save_indicators:
                logger.info(f"🚫 SKIPPING BOOTSTRAP: Detected new save file (Health: {current_health}/{max_health}, Missiles: {current_missiles}/{max_missiles})")
                return
            
            # If either phase is detected AND we're not in a new save, try bootstrap with raw memory
            if mb1_current or mb2_current:
                logger.info(f"🔄 MB phases detected in current state: MB1={mb1_current}, MB2={mb2_current}")
                logger.info("🔄 Attempting to bootstrap MB cache from current state...")
                
                # Re-read boss memory to get raw data for bootstrap
                memory_data = {
                    'main_bosses': self.udp_reader.read_memory_range(0x7ED828, 2),
                    'crocomire': self.udp_reader.read_memory_range(0x7ED829, 2),
                    'boss_plus_1': self.udp_reader.read_memory_range(0x7ED829, 2),
                    'boss_plus_2': self.udp_reader.read_memory_range(0x7ED82A, 2),
                    'boss_plus_3': self.udp_reader.read_memory_range(0x7ED82B, 2),
                    'boss_plus_4': self.udp_reader.read_memory_range(0x7ED82C, 2),
                    'boss_plus_5': self.udp_reader.read_memory_range(0x7ED82D, 2),
                }
                
                # Use parser's bootstrap method
                self.parser.bootstrap_mb_cache(memory_data, game_state)
                logger.info("✅ MB cache bootstrap completed")
            else:
                logger.info("ℹ️ No MB phases detected in current state - no bootstrap needed")
                
        except Exception as e:
            logger.error(f"Error during MB cache bootstrap: {e}")

class CacheServingHTTPHandler(BaseHTTPRequestHandler):
    """HTTP handler that serves cached data instantly - no UDP blocking"""
    
    def __init__(self, *args, poller=None, **kwargs):
        self.poller = poller
        super().__init__(*args, **kwargs)
    
    def log_message(self, format, *args):
        """Suppress default HTTP logging"""
        pass
    
    def do_GET(self):
        """Handle GET requests"""
        try:
            if self.path == '/':
                self.serve_file('super_metroid_tracker.html')
            elif self.path == '/api/status':
                self.serve_status()
            elif self.path == '/game_state':
                self.serve_game_state()
            elif self.path == '/api/stats':
                self.serve_stats()
            elif self.path == '/api/bootstrap-mb':
                self.serve_bootstrap_mb()
            elif self.path == '/api/manual-mb-complete':
                self.serve_manual_mb_complete()
            elif self.path == '/api/reset-mb-cache':
                self.serve_reset_mb_cache()
            elif self.path == '/api/reset-cache':
                self.serve_reset_cache()
            elif self.path.endswith('.png'):
                self.serve_static_file(self.path[1:], 'image/png')
            else:
                self.send_error(404)
        except Exception as e:
            logger.error(f"Request error: {e}")
            self.send_error(500)
    
    def do_OPTIONS(self):
        """Handle OPTIONS preflight requests for CORS"""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', 'http://localhost:3000')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
    
    def serve_status(self):
        """Serve status from cache - instant response"""
        cached_state = self.poller.get_cached_state()
        self.send_json_response(cached_state)
    
    def serve_game_state(self):
        """Serve game state in format expected by React app"""
        cached_state = self.poller.get_cached_state()
        self.send_json_response(cached_state)
    
    def serve_stats(self):
        """Serve stats from cache - instant response"""
        cached_state = self.poller.get_cached_state()
        stats = cached_state.get('stats', {})
        if not stats:
            stats = {'error': 'No game data available'}
        self.send_json_response(stats)
    
    def serve_bootstrap_mb(self):
        """Serve a dummy response for the bootstrap endpoint"""
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', len(json.dumps({'message': 'Bootstrap triggered'}).encode()))
        self.end_headers()
        self.wfile.write(json.dumps({'message': 'Bootstrap triggered'}).encode())
    
    def serve_manual_mb_complete(self):
        """Manually set MB completion for testing/troubleshooting"""
        try:
            # Force set MB completion in the parser
            if hasattr(self.poller, 'parser'):
                self.poller.parser.mother_brain_phase_state['mb1_detected'] = True
                self.poller.parser.mother_brain_phase_state['mb2_detected'] = True
                message = 'MB1 and MB2 manually set to completed'
                logger.info(f"🔧 Manual MB completion triggered via API")
            else:
                message = 'Parser not available'
            
            response = {'message': message, 'mb1': True, 'mb2': True}
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', len(json.dumps(response).encode()))
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())
        except Exception as e:
            error_response = {'error': str(e)}
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', len(json.dumps(error_response).encode()))
            self.end_headers()
            self.wfile.write(json.dumps(error_response).encode())
    
    def serve_reset_mb_cache(self):
        """Reset Mother Brain cache to default (not detected)"""
        try:
            if hasattr(self.poller, 'parser'):
                self.poller.parser.mother_brain_phase_state = {
                    'mb1_detected': False,
                    'mb2_detected': False
                }
                message = 'MB cache reset to default (not detected)'
                logger.info(f"🔄 MB cache reset via API")
            else:
                message = 'Parser not available'
            
            response = {'message': message}
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', len(json.dumps(response).encode()))
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())
        except Exception as e:
            error_response = {'error': str(e)}
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', len(json.dumps(error_response).encode()))
            self.end_headers()
            self.wfile.write(json.dumps(error_response).encode())
    
    def serve_reset_cache(self):
        """Reset all caches and force fresh game state read"""
        try:
            if hasattr(self.poller, 'parser'):
                # Reset Mother Brain cache
                self.poller.parser.mother_brain_phase_state = {
                    'mb1_detected': False,
                    'mb2_detected': False
                }
                logger.info(f"🔄 MB cache reset via API")
            
            # Clear the background poller's cache to force fresh reads
            if hasattr(self.poller, 'cache_lock') and hasattr(self.poller, 'cache'):
                with self.poller.cache_lock:
                    self.poller.cache['game_state'] = {}
                    logger.info(f"🔄 Background poller cache cleared")
            
            # Force bootstrap flag reset so it will re-bootstrap on next read
            if hasattr(self.poller, 'bootstrap_attempted'):
                self.poller.bootstrap_attempted = False
                logger.info(f"🔄 Bootstrap flag reset - will re-bootstrap on next poll")
            
            message = 'All caches reset - fresh game state will be read on next poll'
            
            response = {'message': message}
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', len(json.dumps(response).encode()))
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())
        except Exception as e:
            error_response = {'error': str(e)}
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', len(json.dumps(error_response).encode()))
            self.end_headers()
            self.wfile.write(json.dumps(error_response).encode())
    
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
    
    def send_json_response(self, data, status_code=200):
        """Send JSON response with CORS headers"""
        json_data = json.dumps(data, indent=2)
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', len(json_data.encode()))
        # Add CORS headers to allow React app access
        self.send_header('Access-Control-Allow-Origin', 'http://localhost:3000')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        self.wfile.write(json_data.encode())

class BackgroundPollerServer:
    """Main server that orchestrates background polling and HTTP serving"""
    
    def __init__(self, port=3000, poll_interval=1.0):
        self.port = port
        self.poll_interval = poll_interval
        self.poller = BackgroundGamePoller(poll_interval)
        self.http_server = None
        
    def start(self):
        """Start the complete server system"""
        try:
            # Start background poller
            self.poller.start()
            
            # Create HTTP server with poller reference
            def handler_factory(*args, **kwargs):
                return CacheServingHTTPHandler(*args, poller=self.poller, **kwargs)
            
            self.http_server = HTTPServer(('localhost', self.port), handler_factory)
            
            logger.info("🚀 Background Polling Super Metroid Tracker Server")
            logger.info("=" * 50)
            logger.info(f"📱 Tracker UI: http://localhost:{self.port}/")
            logger.info(f"📊 API Status: http://localhost:{self.port}/api/status")
            logger.info(f"📈 API Stats:  http://localhost:{self.port}/api/stats")
            logger.info(f"⚡ Background polling: {self.poll_interval}s intervals")
            logger.info(f"🎯 Architecture: Background UDP + Instant Cache Serving")
            logger.info(f"⏹️  Press Ctrl+C to stop")
            logger.info("=" * 50)
            
            # Serve HTTP requests
            self.http_server.serve_forever()
            
        except KeyboardInterrupt:
            logger.info("🛑 Shutdown requested")
        except Exception as e:
            logger.error(f"Server error: {e}")
        finally:
            self.stop()
    
    def stop(self):
        """Stop the server system"""
        if self.poller:
            self.poller.stop()
        if self.http_server:
            self.http_server.shutdown()
            self.http_server.server_close()
        logger.info("🏁 Server stopped")

def signal_handler(signum, frame):
    """Handle shutdown signals"""
    logger.info("🛑 Received shutdown signal")
    sys.exit(0)

if __name__ == "__main__":
    # Handle shutdown signals
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Start server on port 8000 (to avoid conflict with React dev server on 3000)
    server = BackgroundPollerServer(port=8000, poll_interval=1.0)
    server.start() 