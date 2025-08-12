#!/bin/bash

# Test script for Samus Ship detection in Super Metroid HUD Kotlin server
# This script tests the samus_ship flag in the API response

SERVER_URL="http://localhost:8082"

echo "===== Testing Samus Ship Detection ====="
echo "Server URL: $SERVER_URL"
echo ""

# Function to check the samus_ship flag
check_samus_ship() {
    echo "Checking samus_ship flag..."

    # Get the API response
    response=$(curl -s "$SERVER_URL/api/status")

    # Extract the samus_ship flag
    samus_ship=$(echo "$response" | grep -o '"samus_ship":[^,}]*' | cut -d':' -f2)

    echo "samus_ship: $samus_ship"
    echo ""

    # Print other relevant information
    area_id=$(echo "$response" | grep -o '"area_id":[^,}]*' | cut -d':' -f2)
    area_name=$(echo "$response" | grep -o '"area_name":"[^"]*"' | cut -d'"' -f4)
    room_id=$(echo "$response" | grep -o '"room_id":[^,}]*' | cut -d':' -f2)
    player_x=$(echo "$response" | grep -o '"player_x":[^,}]*' | cut -d':' -f2)
    player_y=$(echo "$response" | grep -o '"player_y":[^,}]*' | cut -d':' -f2)

    echo "Location: $area_name (area_id: $area_id, room_id: $room_id)"
    echo "Position: ($player_x, $player_y)"

    # Check Mother Brain status
    mb=$(echo "$response" | grep -o '"mother_brain":[^,}]*' | cut -d':' -f2)
    mb1=$(echo "$response" | grep -o '"mother_brain_1":[^,}]*' | cut -d':' -f2)
    mb2=$(echo "$response" | grep -o '"mother_brain_2":[^,}]*' | cut -d':' -f2)

    echo "Mother Brain: $mb, MB1: $mb1, MB2: $mb2"

    # Check ship_ai and event_flags (if available in logs)
    echo ""
    echo "Checking logs for ship_ai and event_flags..."
    tail -n 50 ../kotlin-server.log | grep -E "🚢|Ship"
}

# Main function
main() {
    echo "===== Test 1: Initial Check ====="
    check_samus_ship

    echo ""
    echo "===== Test 2: After Reset ====="
    echo "Resetting cache..."
    curl -s -X POST "$SERVER_URL/api/reset-cache" > /dev/null
    sleep 2
    check_samus_ship

    echo ""
    echo "===== Instructions ====="
    echo "1. Load a new game save file and run this script again"
    echo "2. Load a started but not completed save file and run this script again"
    echo "3. Load a completed save file and run this script again"
    echo ""
    echo "To run this script again:"
    echo "  bash explore/test_samus_ship.sh"
}

# Run the main function
main
