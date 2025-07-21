#!/usr/bin/env python3
"""
Super Metroid Tracker Server Watchdog

Monitors the unified tracker server health and automatically restarts it
when it becomes unresponsive or unhealthy.
"""

import time
import subprocess
import requests
import logging
import signal
import sys
import os
from typing import Optional

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('watchdog.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class ServerWatchdog:
    def __init__(self):
        self.server_url = "http://localhost:3000"
        self.health_check_interval = 10  # seconds
        self.max_unhealthy_time = 30    # seconds before restart
        self.server_process = None
        self.last_healthy_time = time.time()
        self.restart_count = 0
        self.max_restarts_per_hour = 10
        self.restart_times = []
        
    def is_server_responsive(self) -> bool:
        """Check if server is responding to HTTP requests"""
        try:
            response = requests.get(f"{self.server_url}/api/status", timeout=5)
            return response.status_code == 200
        except Exception as e:
            logger.debug(f"Server not responsive: {e}")
            return False
    
    def get_server_health(self) -> Optional[dict]:
        """Get detailed server health status"""
        try:
            response = requests.get(f"{self.server_url}/api/status", timeout=5)
            if response.status_code == 200:
                data = response.json()
                return data.get('health', {})
        except Exception as e:
            logger.debug(f"Failed to get health status: {e}")
        return None
    
    def is_server_healthy(self) -> bool:
        """Check if server is healthy based on health endpoint"""
        health = self.get_server_health()
        if not health:
            return False
            
        # Check various health indicators
        is_healthy = health.get('is_healthy', False)
        circuit_breaker_state = health.get('circuit_breaker_state', 'unknown')
        consecutive_failures = health.get('consecutive_failures', 0)
        time_since_success = health.get('time_since_last_success', float('inf'))
        
        # Consider unhealthy if:
        # - Health status is False
        # - Circuit breaker has been open for too long
        # - Too many consecutive failures
        # - Too long since last successful read
        if not is_healthy:
            logger.warning(f"Server marked unhealthy by health system")
            return False
            
        if circuit_breaker_state == 'open' and time_since_success > 60:
            logger.warning(f"Circuit breaker open for {time_since_success:.1f}s")
            return False
            
        if consecutive_failures >= 5:
            logger.warning(f"Too many consecutive failures: {consecutive_failures}")
            return False
            
        return True
    
    def start_server(self) -> bool:
        """Start the unified tracker server"""
        try:
            if self.server_process:
                self.stop_server()
                
            logger.info("🚀 Starting unified tracker server...")
            
            # Activate virtual environment and start server
            cmd = ["python", "unified_tracker_server.py"]
            
            self.server_process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                cwd=os.getcwd()
            )
            
            # Wait a bit for server to start
            time.sleep(3)
            
            # Check if it started successfully
            if self.is_server_responsive():
                logger.info("✅ Server started successfully")
                self.restart_count += 1
                self.restart_times.append(time.time())
                self.last_healthy_time = time.time()
                return True
            else:
                logger.error("❌ Server failed to start properly")
                return False
                
        except Exception as e:
            logger.error(f"Failed to start server: {e}")
            return False
    
    def stop_server(self):
        """Stop the unified tracker server"""
        try:
            # Kill any existing unified_tracker_server.py processes
            subprocess.run(["pkill", "-f", "unified_tracker_server.py"], 
                         capture_output=True)
            
            if self.server_process:
                self.server_process.terminate()
                try:
                    self.server_process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    self.server_process.kill()
                    self.server_process.wait()
                self.server_process = None
                
            logger.info("🛑 Server stopped")
            
        except Exception as e:
            logger.error(f"Error stopping server: {e}")
    
    def should_restart(self) -> bool:
        """Check if we should restart based on rate limiting"""
        now = time.time()
        
        # Remove restart times older than 1 hour
        self.restart_times = [t for t in self.restart_times if now - t < 3600]
        
        if len(self.restart_times) >= self.max_restarts_per_hour:
            logger.error(f"🚫 Too many restarts ({len(self.restart_times)}) in the last hour")
            return False
            
        return True
    
    def run(self):
        """Main watchdog loop"""
        logger.info("🐕 Server watchdog starting...")
        
        # Start server initially
        if not self.start_server():
            logger.error("Failed to start server initially")
            return
            
        try:
            while True:
                time.sleep(self.health_check_interval)
                
                responsive = self.is_server_responsive()
                healthy = self.is_server_healthy() if responsive else False
                
                if responsive and healthy:
                    self.last_healthy_time = time.time()
                    logger.debug("✅ Server healthy")
                else:
                    unhealthy_duration = time.time() - self.last_healthy_time
                    
                    if not responsive:
                        logger.warning(f"🔴 Server unresponsive for {unhealthy_duration:.1f}s")
                    else:
                        logger.warning(f"🟡 Server unhealthy for {unhealthy_duration:.1f}s")
                    
                    # Restart if unhealthy for too long
                    if unhealthy_duration > self.max_unhealthy_time:
                        if self.should_restart():
                            logger.warning(f"🔄 Restarting server (unhealthy for {unhealthy_duration:.1f}s)")
                            self.start_server()
                        else:
                            logger.error("🚫 Cannot restart - rate limit exceeded")
                            break
                            
        except KeyboardInterrupt:
            logger.info("🛑 Watchdog interrupted by user")
        except Exception as e:
            logger.error(f"🚨 Watchdog error: {e}")
        finally:
            self.stop_server()
            logger.info("🐕 Watchdog stopped")

def signal_handler(signum, frame):
    """Handle shutdown signals"""
    logger.info("🛑 Received shutdown signal")
    sys.exit(0)

if __name__ == "__main__":
    # Handle shutdown signals
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    watchdog = ServerWatchdog()
    watchdog.run() 