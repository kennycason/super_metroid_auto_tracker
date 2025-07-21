#!/usr/bin/env python3
"""
Super Metroid Tracker Web Server
Serves UDP tracker data via HTTP JSON API
ENHANCED: Better error handling and crash resistance
"""

import json
import threading
import time
import logging
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler
from super_metroid_udp_tracker import SuperMetroidUDPTracker

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('tracker_server.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class TrackerHandler(BaseHTTPRequestHandler):
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
                self.serve_file(self.path[1:])  # Remove leading /
            elif self.path.endswith('.png'):
                self.serve_static_file(self.path[1:], 'image/png')  # Serve PNG images
            elif self.path.endswith('.jpg') or self.path.endswith('.jpeg'):
                self.serve_static_file(self.path[1:], 'image/jpeg')  # Serve JPEG images
            elif self.path.endswith('.css'):
                self.serve_static_file(self.path[1:], 'text/css')  # Serve CSS files
            elif self.path.endswith('.js'):
                self.serve_static_file(self.path[1:], 'application/javascript')  # Serve JS files
            else:
                self.send_error(404)
        except Exception as e:
            logger.error(f"Error handling request {self.path}: {e}")
            self.send_error(500)
            
    def do_OPTIONS(self):
        """Handle CORS preflight requests"""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        
    def serve_status(self):
        """Serve tracker status as JSON"""
        try:
            status = self.tracker.get_status()
            
            # RESILIENCE: Always provide valid response even if tracker fails
            if not status or 'connected' not in status:
                status = {
                    'connected': False,
                    'game_loaded': False,
                    'retroarch_version': None,
                    'game_info': 'Connection failed',
                    'stats': {},
                    'error': 'Tracker connection failed'
                }
                logger.warning("Tracker returned invalid status, using fallback")
                
            self.send_json_response(status)
        except Exception as e:
            logger.error(f"Error getting tracker status: {e}")
            fallback_status = {
                'connected': False,
                'game_loaded': False,
                'retroarch_version': None,
                'game_info': 'Server error',
                'stats': {},
                'error': f'Server error: {str(e)}'
            }
            self.send_json_response(fallback_status, 500)
            
    def serve_stats(self):
        """Serve just the stats as JSON"""
        try:
            status = self.tracker.get_status()
            
            # RESILIENCE: Always provide valid stats structure
            if status and 'stats' in status and status['stats']:
                stats = status['stats']
            else:
                # Fallback with empty but valid structure
                stats = {
                    'health': 0, 'max_health': 0,
                    'missiles': 0, 'max_missiles': 0, 
                    'supers': 0, 'max_supers': 0,
                    'power_bombs': 0, 'max_power_bombs': 0,
                    'reserve_energy': 0, 'max_reserve_energy': 0,
                    'room_id': 0, 'area_id': 0, 'area_name': 'Unknown',
                    'game_state': 0, 'player_x': 0, 'player_y': 0,
                    'items': {}, 'beams': {}, 'bosses': {},
                    'error': 'No game data available'
                }
                logger.warning("No stats available, using fallback")
                
            self.send_json_response(stats)
        except Exception as e:
            logger.error(f"Error getting tracker stats: {e}")
            fallback_stats = {
                'error': f'Stats error: {str(e)}',
                'health': 0, 'max_health': 0,
                'items': {}, 'beams': {}, 'bosses': {}
            }
            self.send_json_response(fallback_stats, 500)
            
    def serve_file(self, filename):
        """Serve static files"""
        try:
            with open(filename, 'r') as f:
                content = f.read()
            
            # Determine content type
            if filename.endswith('.html'):
                content_type = 'text/html'
            elif filename.endswith('.js'):
                content_type = 'application/javascript'
            elif filename.endswith('.css'):
                content_type = 'text/css'
            else:
                content_type = 'text/plain'
                
            self.send_response(200)
            self.send_header('Content-Type', content_type)
            self.send_header('Content-Length', len(content.encode()))
            self.end_headers()
            self.wfile.write(content.encode())
        except FileNotFoundError:
            logger.warning(f"File not found: {filename}")
            self.send_error(404)
        except Exception as e:
            logger.error(f"Error serving file {filename}: {e}")
            self.send_error(500)
            
    def serve_static_file(self, filename, content_type):
        """Serve static files (images, CSS, JS) as binary data"""
        try:
            with open(filename, 'rb') as f:  # Open in binary mode for images
                content = f.read()
                
            self.send_response(200)
            self.send_header('Content-Type', content_type)
            self.send_header('Content-Length', len(content))
            self.end_headers()
            self.wfile.write(content)  # Write binary data directly
        except FileNotFoundError:
            logger.warning(f"Static file not found: {filename}")
            self.send_error(404)
        except Exception as e:
            logger.error(f"Error serving static file {filename}: {e}")
            self.send_error(500)
            
    def send_json_response(self, data, status_code=200):
        """Send JSON response with CORS headers"""
        try:
            json_data = json.dumps(data, indent=2)
            self.send_response(status_code)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Content-Length', len(json_data.encode()))
            self.end_headers()
            self.wfile.write(json_data.encode())
        except Exception as e:
            logger.error(f"Error sending JSON response: {e}")
            # Last resort: send basic error
            error_msg = '{"error": "JSON encoding failed"}'
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', len(error_msg))
            self.end_headers()
            self.wfile.write(error_msg.encode())
        
    def log_message(self, format, *args):
        """Override to reduce log noise but keep important messages"""
        if any(x in format % args for x in ['error', 'Error', 'ERROR']):
            logger.warning(f"HTTP: {format % args}")
        # Suppress normal request logging to reduce noise

class TrackerWebServer:
    def __init__(self, port=8080):
        self.port = port
        self.tracker = SuperMetroidUDPTracker()
        self.server = None
        self.running = False
        self.last_connection_check = 0
        
    def ensure_connection(self):
        """Ensure tracker connection is alive, reconnect if needed"""
        current_time = time.time()
        
        # Only check connection every 30 seconds to avoid spam
        if current_time - self.last_connection_check < 30:
            return
            
        self.last_connection_check = current_time
        
        try:
            # Test connection
            status = self.tracker.get_status()
            if not status or not status.get('connected', False):
                logger.warning("Tracker connection lost, attempting reconnect...")
                self.tracker.disconnect()
                if self.tracker.connect():
                    logger.info("✅ Tracker reconnected successfully")
                else:
                    logger.error("❌ Failed to reconnect tracker")
        except Exception as e:
            logger.error(f"Error checking tracker connection: {e}")
        
    def start(self):
        """Start the web server"""
        logger.info("🚀 Starting Super Metroid Tracker Server...")
        
        # Initial connection
        if not self.tracker.connect():
            logger.warning("⚠️ Could not connect to RetroArch initially, will retry automatically")
            
        def handler(*args, **kwargs):
            # Ensure connection on each request (with rate limiting)
            self.ensure_connection()
            TrackerHandler(*args, tracker=self.tracker, **kwargs)
            
        try:
            self.server = HTTPServer(('localhost', self.port), handler)
            self.running = True
            
            logger.info(f"✅ Server started successfully:")
            logger.info(f"   📱 Tracker UI: http://localhost:{self.port}/")
            logger.info(f"   📊 API Status: http://localhost:{self.port}/api/status") 
            logger.info(f"   📈 API Stats:  http://localhost:{self.port}/api/stats")
            logger.info(f"   ⏹️  Press Ctrl+C to stop")
            
            self.server.serve_forever()
        except KeyboardInterrupt:
            logger.info("⚠️ Server stopped by user")
        except Exception as e:
            logger.error(f"❌ Server error: {e}")
            logger.info("🔄 Server will restart automatically...")
        finally:
            self.stop()
            
    def stop(self):
        """Stop the web server"""
        logger.info("🛑 Stopping tracker server...")
        self.running = False
        if self.server:
            try:
                self.server.shutdown()
                self.server.server_close()
            except Exception as e:
                logger.error(f"Error stopping server: {e}")
        if self.tracker:
            try:
                self.tracker.disconnect()
            except Exception as e:
                logger.error(f"Error disconnecting tracker: {e}")

def main():
    """Start the tracker web server with auto-restart"""
    max_restarts = 5
    restart_count = 0
    
    while restart_count < max_restarts:
        try:
            logger.info(f"🚀 Starting tracker server (attempt {restart_count + 1})")
            server = TrackerWebServer(port=3000)
            server.start()
            break  # If we get here, server stopped normally
        except KeyboardInterrupt:
            logger.info("👋 Goodbye!")
            break
        except Exception as e:
            restart_count += 1
            logger.error(f"❌ Server crashed: {e}")
            if restart_count < max_restarts:
                wait_time = min(5 * restart_count, 30)  # Progressive backoff
                logger.info(f"🔄 Restarting in {wait_time} seconds... ({restart_count}/{max_restarts})")
                time.sleep(wait_time)
            else:
                logger.error("❌ Max restarts reached. Server stopped.")
                break

if __name__ == "__main__":
    main() 