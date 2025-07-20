#!/usr/bin/env python3
"""
Super Metroid Tracker Web Server
Serves UDP tracker data via HTTP JSON API
"""

import json
import threading
import time
from http.server import HTTPServer, BaseHTTPRequestHandler
from super_metroid_udp_tracker import SuperMetroidUDPTracker

class TrackerHandler(BaseHTTPRequestHandler):
    def __init__(self, *args, tracker=None, **kwargs):
        self.tracker = tracker
        super().__init__(*args, **kwargs)
        
    def do_GET(self):
        """Handle GET requests"""
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
            self.send_json_response(status)
        except Exception as e:
            self.send_json_response({'error': str(e)}, 500)
            
    def serve_stats(self):
        """Serve just the stats as JSON"""
        try:
            status = self.tracker.get_status()
            stats = status.get('stats', {})
            self.send_json_response(stats)
        except Exception as e:
            self.send_json_response({'error': str(e)}, 500)
            
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
            self.send_error(404)
        except Exception as e:
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
            self.send_error(404)
        except Exception as e:
            self.send_error(500)
            
    def send_json_response(self, data, status_code=200):
        """Send JSON response with CORS headers"""
        json_data = json.dumps(data, indent=2)
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Content-Length', len(json_data.encode()))
        self.end_headers()
        self.wfile.write(json_data.encode())
        
    def log_message(self, format, *args):
        """Override to reduce log noise"""
        pass

class TrackerWebServer:
    def __init__(self, port=8080):
        self.port = port
        self.tracker = SuperMetroidUDPTracker()
        self.server = None
        self.running = False
        
    def start(self):
        """Start the web server"""
        if not self.tracker.connect():
            print("❌ Could not connect to RetroArch")
            return False
            
        def handler(*args, **kwargs):
            TrackerHandler(*args, tracker=self.tracker, **kwargs)
            
        try:
            self.server = HTTPServer(('localhost', self.port), handler)
            self.running = True
            
            print(f"🚀 Super Metroid Tracker Server running at:")
            print(f"   📱 Tracker UI: http://localhost:{self.port}/")
            print(f"   📊 API Status: http://localhost:{self.port}/api/status")
            print(f"   📈 API Stats:  http://localhost:{self.port}/api/stats")
            print(f"   ⏹️  Press Ctrl+C to stop")
            
            self.server.serve_forever()
        except KeyboardInterrupt:
            print("\n⚠️ Server stopped by user")
        except Exception as e:
            print(f"❌ Server error: {e}")
        finally:
            self.stop()
            
    def stop(self):
        """Stop the web server"""
        self.running = False
        if self.server:
            self.server.shutdown()
            self.server.server_close()
        if self.tracker:
            self.tracker.disconnect()

def main():
    """Start the tracker web server"""
    server = TrackerWebServer(port=3000)
    try:
        server.start()
    except KeyboardInterrupt:
        print("\n👋 Goodbye!")

if __name__ == "__main__":
    main() 