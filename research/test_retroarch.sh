#!/bin/bash

# Simple RetroArch Network API Test Script
# Just tests UDP connection - no config checking

echo "============================================================"
echo "🎮 RetroArch Network API Connection Test"
echo "============================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to test RetroArch UDP API
test_retroarch_udp() {
    echo -e "\n${BLUE}🔌 Testing RetroArch UDP API at localhost:55355${NC}"
    
    # Create a temporary file for the response
    local temp_file=$(mktemp)
    
    # Use Python for UDP communication since netcat doesn't handle UDP responses well
    python3 -c "
import socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.settimeout(3)
try:
    message = 'READ_CORE_MEMORY 007e0998 2'.encode('ascii')
    sock.sendto(message, ('localhost', 55355))
    response, addr = sock.recvfrom(1024)
    print(f'Response from {addr}: {len(response)} bytes')
    print(f'Data: {response[:10]}')
    if len(response) >= 2:
        value = int.from_bytes(response[:2], 'little')
        print(f'Game State: {value} (0x{value:04x})')
    else:
        print('Invalid response')
except Exception as e:
    print(f'Error: {e}')
finally:
    sock.close()
" > "$temp_file" 2>&1
    
    # Check if we got a response
    if [ -s "$temp_file" ]; then
        local response_size=$(wc -c < "$temp_file")
        echo "   📥 Response: $response_size bytes"
        
        # Check if the response contains an error message
        if grep -q "Error:" "$temp_file"; then
            echo -e "   ${RED}❌ Python error detected${NC}"
            echo "   📊 Error details:"
            cat "$temp_file"
            rm -f "$temp_file"
            return 1
        fi
        
        # Check if the response contains "Game State" (success indicator)
        if grep -q "Game State:" "$temp_file"; then
            echo -e "   ${GREEN}✅ Success! RetroArch UDP API is working${NC}"
            echo "   📊 Response data:"
            cat "$temp_file"
            rm -f "$temp_file"
            return 0
        else
            echo -e "   ${YELLOW}⚠️  Unexpected response format${NC}"
            echo "   📊 Response data:"
            cat "$temp_file"
            rm -f "$temp_file"
            return 1
        fi
    else
        echo -e "   ${YELLOW}⏰ No response received${NC}"
        echo "   💡 This could mean:"
        echo "      • RetroArch is running but no ROM is loaded"
        echo "      • Network Commands are disabled"
        echo "      • Wrong port or RetroArch not responding"
        rm -f "$temp_file"
        return 1
    fi
}

# Function to show troubleshooting
show_troubleshooting() {
    echo -e "\n${YELLOW}💡 TROUBLESHOOTING:${NC}"
    echo "   1. Make sure RetroArch is running with a Super Metroid ROM loaded"
    echo "   2. Enable Network Commands in RetroArch:"
    echo "      • Settings → Network → Network Commands = ON"
    echo "      • Default port should be 55355"
    echo "   3. Check if netcat is installed: apt-get install netcat-openbsd"
    echo "   4. Check firewall settings"
}

# Main execution
main() {
    # Check if netcat is available
    if ! command -v nc >/dev/null 2>&1; then
        echo -e "${RED}❌ Error: netcat (nc) is required but not installed${NC}"
        echo "Install with:"
        echo "  Ubuntu/Debian: sudo apt-get install netcat-openbsd"
        echo "  CentOS/RHEL: sudo yum install nc"
        echo "  macOS: brew install netcat"
        exit 1
    fi
    
    # Test UDP API
    test_retroarch_udp
    local udp_ok=$?
    
    # Summary
    echo -e "\n============================================================"
    echo -e "${BLUE}📊 SUMMARY${NC}"
    echo -e "============================================================"
    
    if [ $udp_ok -eq 0 ]; then
        echo -e "${GREEN}✅ RetroArch UDP API: WORKING${NC}"
        echo "   • Super Metroid tracker can connect via RetroArch"
    else
        echo -e "${RED}❌ RetroArch UDP API: NOT WORKING${NC}"
        echo "   • Make sure RetroArch is running with Network Commands enabled"
        show_troubleshooting
    fi
    
    echo -e "\n============================================================"
}

# Run the test
main