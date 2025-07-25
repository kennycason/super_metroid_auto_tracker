#!/bin/bash
# 🎯 Super Metroid Kotlin Server Management Script
# Properly manages server on a CONSISTENT port without hopping

set -e

PORT=8081
POLL_INTERVAL=1000

echo "🎮 Super Metroid Kotlin Server Manager"
echo "======================================"

# Step 1: Kill any existing server processes
echo "🧹 Cleaning up existing server processes..."
pkill -9 -f "server.kexe" 2>/dev/null || true
sleep 2

# Step 2: Check port availability
echo "🔍 Checking port $PORT..."
if lsof -ti:$PORT >/dev/null 2>&1; then
    echo "⚠️  Port $PORT is still occupied, force killing..."
    lsof -ti:$PORT | xargs kill -9 2>/dev/null || true
    sleep 2
fi

# Step 3: Build server if needed
echo "🔨 Building server..."
cd "$(dirname "$0")"
./gradlew linkDebugExecutableNative

# Step 4: Start server
echo "🚀 Starting server on port $PORT..."
./build/bin/native/debugExecutable/server.kexe $PORT $POLL_INTERVAL &
SERVER_PID=$!

# Step 5: Wait for startup
echo "⏳ Waiting for server startup..."
sleep 3

# Step 6: Test connectivity
if curl -s http://localhost:$PORT/health >/dev/null; then
    echo "✅ Server started successfully!"
    echo "📊 API Status: http://localhost:$PORT/api/status"
    echo "💻 React App: http://localhost:3001/ (update BACKEND_URL if needed)"
    echo "🔧 Server PID: $SERVER_PID"
else
    echo "❌ Server failed to start properly"
    exit 1
fi 