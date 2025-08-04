#!/bin/bash

# Kotlin Super Metroid Tracker Server Management
# Manages Kotlin server

KOTLIN_PORT=8081 # DO NOT CHANGE
KOTLIN_LOG="kotlin-server.log"
KOTLIN_EXECUTABLE="./build/bin/native/debugExecutable/server_kotlin.kexe"
POLL_INTERVAL=1000

case "$1" in
    start)
        echo "🚀 Starting Kotlin Super Metroid Tracker server..."

        # Kill any existing Kotlin server processes
        echo "🧹 Cleaning up existing Kotlin processes..."
        pkill -9 -f "server_kotlin.kexe" 2>/dev/null
        pkill -9 -f "$KOTLIN_PORT" 2>/dev/null

        # Check port availability
        if lsof -ti:$KOTLIN_PORT >/dev/null 2>&1; then
            echo "⚠️  Port $KOTLIN_PORT is still occupied, force killing..."
            lsof -ti:$KOTLIN_PORT | xargs kill -9 2>/dev/null || true

            # Wait a bit longer for the port to be released
            echo "⏳ Waiting for port to be released..."
            sleep 5

            # Verify port is now available
            if lsof -ti:$KOTLIN_PORT >/dev/null 2>&1; then
                echo "❌ Port $KOTLIN_PORT is still in use after cleanup attempts."
                echo "   Please manually check what process is using this port:"
                echo "   lsof -i:$KOTLIN_PORT"
                echo "   Then kill that process or choose a different port."
                exit 1
            else
                echo "✅ Port $KOTLIN_PORT is now available."
            fi
        else
            echo "✅ Port $KOTLIN_PORT is available."
        fi

        # Build server
        echo "🔨 Building Kotlin server..."
        cd "$(dirname "$0")"
        if ./gradlew linkDebugExecutableNative; then
            echo "✅ Build successful"
        else
            echo "❌ Build failed!"
            exit 1
        fi

        # Start Kotlin server
        echo "⚡ Starting Kotlin server on port $KOTLIN_PORT..."
        if [ -f "$KOTLIN_EXECUTABLE" ]; then
            $KOTLIN_EXECUTABLE $KOTLIN_PORT $POLL_INTERVAL > "$KOTLIN_LOG" 2>&1 &
            KOTLIN_PID=$!
            echo "   Kotlin PID: $KOTLIN_PID"
            sleep 3

            # Test server
            echo "🔍 Testing Kotlin server..."
            if curl -s http://localhost:$KOTLIN_PORT/api/status > /dev/null; then
                echo "✅ Kotlin server (port $KOTLIN_PORT) - RUNNING"
            elif curl -s http://localhost:$KOTLIN_PORT/health > /dev/null; then
                echo "✅ Kotlin server (port $KOTLIN_PORT) - RUNNING (health check)"
            else
                echo "❌ Kotlin server (port $KOTLIN_PORT) - FAILED"
            fi
        else
            echo "❌ Error: $KOTLIN_EXECUTABLE not found! Build may have failed."
            exit 1
        fi

        echo "📝 Log: $KOTLIN_LOG"
        echo "📊 API Status: http://localhost:$KOTLIN_PORT/api/status"
        ;;

    stop)
        echo "🛑 Stopping Kotlin server..."
        pkill -f "server_kotlin.kexe"
        pkill -f "$KOTLIN_PORT"

        # Verify server has stopped
        sleep 2
        if pgrep -f "server_kotlin.kexe" > /dev/null; then
            echo "⚠️ Server still running, force killing..."
            pkill -9 -f "server_kotlin.kexe"
            sleep 1
        fi

        echo "✅ Kotlin server stopped"
        ;;

    restart)
        echo "🔄 Restarting Kotlin server..."
        $0 stop
        sleep 2
        $0 start
        ;;

    build)
        echo "🔨 Building Kotlin server..."
        cd "$(dirname "$0")"
        ./gradlew linkDebugExecutableNative
        ;;

    clean)
        echo "🧹 Cleaning Kotlin build..."
        cd "$(dirname "$0")"
        ./gradlew clean
        ;;

    status)
        echo "📊 Kotlin Server Status:"
        if pgrep -f "server_kotlin.kexe" > /dev/null; then
            echo "⚡ Kotlin server - RUNNING (port $KOTLIN_PORT)"
            echo "   PID: $(pgrep -f "server_kotlin.kexe")"

            # Check if the port is actually responding
            if curl -s --connect-timeout 2 http://localhost:$KOTLIN_PORT/health > /dev/null; then
                echo "   Health check: ✅ RESPONDING"
            else
                echo "   Health check: ❌ NOT RESPONDING (process running but port not responding)"
            fi
        else
            echo "⚡ Kotlin server - STOPPED"

            # Check if port is in use by another process
            if lsof -ti:$KOTLIN_PORT > /dev/null 2>&1; then
                echo "⚠️  Port $KOTLIN_PORT is in use by another process:"
                lsof -i:$KOTLIN_PORT
            fi
        fi
        ;;

    logs)
        echo "📋 Kotlin server logs (last 20 lines):"
        if [ -f "$KOTLIN_LOG" ]; then
            tail -n 20 "$KOTLIN_LOG"
        else
            echo "No Kotlin logs found ($KOTLIN_LOG)"
        fi
        ;;

    tail)
        echo "📋 Following Kotlin server logs (Ctrl+C to exit):"
        if [ -f "$KOTLIN_LOG" ]; then
            tail -f "$KOTLIN_LOG"
        else
            echo "No Kotlin logs found ($KOTLIN_LOG)"
        fi
        ;;

    test)
        echo "🧪 Testing Kotlin API response..."
        if curl -s http://localhost:$KOTLIN_PORT/api/status > /dev/null; then
            echo "=== KOTLIN SERVER (port $KOTLIN_PORT) ==="
            curl -s http://localhost:$KOTLIN_PORT/api/status | jq -c '{connected, game_loaded, poll_count, stats: {health: .stats.health, bosses: .stats.bosses}}'
        else
            echo "❌ Kotlin server not responding on port $KOTLIN_PORT"
        fi
        ;;

    *)
        echo "Kotlin Super Metroid Tracker Server Management"
        echo ""
        echo "Usage: $0 {start|stop|restart|build|clean|status|logs|tail|test}"
        echo ""
        echo "Commands:"
        echo "  start   - Build and start Kotlin server (port $KOTLIN_PORT)"
        echo "  stop    - Stop Kotlin server"
        echo "  restart - Restart Kotlin server"
        echo "  build   - Build Kotlin server only"
        echo "  clean   - Clean Kotlin build artifacts"
        echo "  status  - Show server status"
        echo "  logs    - Show recent logs"
        echo "  tail    - Follow logs in real-time"
        echo "  test    - Test API response"
        echo ""
        echo "Server URL: http://localhost:$KOTLIN_PORT/api/status"
        ;;
esac
