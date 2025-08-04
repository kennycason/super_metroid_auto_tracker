#!/bin/bash

# Debug script for Golden Torizo detection
# This script runs the server and captures logs related to Golden Torizo detection

# Set log file
LOG_FILE="golden_torizo_debug.log"

echo "🏆 Golden Torizo Detection Debug Script"
echo "======================================="
echo "This script will run the server and capture logs related to Golden Torizo detection."
echo "Log file: $LOG_FILE"
echo ""

# Clean up any existing log file
if [ -f "$LOG_FILE" ]; then
    echo "Removing existing log file..."
    rm "$LOG_FILE"
fi

# Stop any running server
echo "Stopping any running server..."
cd "$(dirname "$0")/.."
bash manage_server.sh stop

# Start the server
echo "Starting server with debug logging..."
bash manage_server.sh start

# Wait for the server to start
echo "Waiting for server to start..."
sleep 5

# Make a request to the API to trigger the Golden Torizo detection
echo "Making request to API to trigger Golden Torizo detection..."
curl -s http://localhost:8082/api/status > /dev/null

# Wait a moment for logs to be generated
sleep 2

# Extract Golden Torizo debug logs
echo "Extracting Golden Torizo debug logs..."
grep -a "GOLDEN TORIZO DEBUG" kotlin-server.log > "$LOG_FILE"

# Display the logs
echo ""
echo "Golden Torizo Debug Logs:"
echo "========================="
cat "$LOG_FILE"
echo ""
echo "Debug logs saved to $LOG_FILE"

# Provide instructions for the next steps
echo ""
echo "Next Steps:"
echo "1. Analyze these logs to understand why Golden Torizo is being incorrectly detected"
echo "2. Load a save file where Golden Torizo is actually defeated"
echo "3. Run this script again to capture logs for comparison"
echo "4. Compare the logs to identify the issue"
echo ""
