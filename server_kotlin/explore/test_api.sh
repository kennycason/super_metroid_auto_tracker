#!/bin/bash

# Test script for Super Metroid HUD Kotlin server API
# This script tests the /api/status and /api/reset-cache endpoints

SERVER_URL="http://localhost:8081"

echo "===== Testing Super Metroid HUD Kotlin Server API ====="
echo "Server URL: $SERVER_URL"
echo ""

# Function to make API calls and display response
call_api() {
    local endpoint=$1
    local method=${2:-GET}

    echo "Calling $method $endpoint..."
    if [ "$method" = "GET" ]; then
        curl -s "$SERVER_URL$endpoint" | jq .
    else
        curl -s -X "$method" "$SERVER_URL$endpoint"
    fi
    echo ""
}

# Test /api/status endpoint
echo "===== Testing /api/status endpoint ====="
call_api "/api/status"

# Test /api/reset-cache endpoint
echo "===== Testing /api/reset-cache endpoint ====="
call_api "/api/reset-cache" "POST"

# Test /api/status again to see if cache was reset
echo "===== Testing /api/status after reset ====="
call_api "/api/status"

# Test /api/reset endpoint (which doesn't exist but user is trying to use)
echo "===== Testing /api/reset endpoint ====="
call_api "/api/reset" "GET"

# Test /api/reset-mb-cache endpoint
echo "===== Testing /api/reset-mb-cache endpoint ====="
call_api "/api/reset-mb-cache" "GET"

echo "===== Tests completed ====="
