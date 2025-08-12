#!/bin/bash

# Python Super Metroid Tracker Server Management
# Manages Python server on port 8081

PYTHON_PORT=8081
PYTHON_LOG="python-server.log"
PYTHON_SCRIPT="background_poller_server.py"

case "$1" in
    start)
        echo "🚀 Starting Python Super Metroid Tracker server..."
        
        # Kill any existing Python server processes
        echo "🧹 Cleaning up existing Python processes..."
        pkill -9 -f "$PYTHON_SCRIPT" 2>/dev/null
        pkill -9 -f "$PYTHON_PORT" 2>/dev/null
        sleep 2
        
        # Start Python server
        echo "🐍 Starting Python server on port $PYTHON_PORT..."
        if [ -f "$PYTHON_SCRIPT" ]; then
            python3 "$PYTHON_SCRIPT" > "$PYTHON_LOG" 2>&1 &
            PYTHON_PID=$!
            echo "   Python PID: $PYTHON_PID"
            sleep 3
            
            # Test server
            echo "🔍 Testing Python server..."
            if curl -s http://localhost:$PYTHON_PORT/api/status > /dev/null; then
                echo "✅ Python server (port $PYTHON_PORT) - RUNNING"
            else
                echo "❌ Python server (port $PYTHON_PORT) - FAILED"
            fi
        else
            echo "❌ Error: $PYTHON_SCRIPT not found in current directory!"
            exit 1
        fi
        
        echo "📝 Log: $PYTHON_LOG"
        ;;
        
    stop)
        echo "🛑 Stopping Python server..."
        pkill -f "$PYTHON_SCRIPT"
        pkill -f "$PYTHON_PORT"
        echo "✅ Python server stopped"
        ;;
        
    restart)
        echo "🔄 Restarting Python server..."
        $0 stop
        sleep 2
        $0 start
        ;;
        
    status)
        echo "📊 Python Server Status:"
        if pgrep -f "$PYTHON_SCRIPT" > /dev/null; then
            echo "🐍 Python server - RUNNING (port $PYTHON_PORT)"
            echo "   PID: $(pgrep -f "$PYTHON_SCRIPT")"
        else
            echo "🐍 Python server - STOPPED"
        fi
        ;;
        
    logs)
        echo "📋 Python server logs (last 20 lines):"
        if [ -f "$PYTHON_LOG" ]; then
            tail -n 20 "$PYTHON_LOG"
        else
            echo "No Python logs found ($PYTHON_LOG)"
        fi
        ;;
        
    tail)
        echo "📋 Following Python server logs (Ctrl+C to exit):"
        if [ -f "$PYTHON_LOG" ]; then
            tail -f "$PYTHON_LOG"
        else
            echo "No Python logs found ($PYTHON_LOG)"
        fi
        ;;
        
    test)
        echo "🧪 Testing Python API response..."
        if curl -s http://localhost:$PYTHON_PORT/api/status > /dev/null; then
            echo "=== PYTHON SERVER (port $PYTHON_PORT) ==="
            curl -s http://localhost:$PYTHON_PORT/api/status | jq -c '{connected, game_loaded, poll_count, stats: {health: .stats.health, bosses: .stats.bosses}}'
        else
            echo "❌ Python server not responding on port $PYTHON_PORT"
        fi
        ;;
        
    *)
        echo "Python Super Metroid Tracker Server Management"
        echo ""
        echo "Usage: $0 {start|stop|restart|status|logs|tail|test}"
        echo ""
        echo "Commands:"
        echo "  start   - Start Python server (port $PYTHON_PORT)"
        echo "  stop    - Stop Python server"
        echo "  restart - Restart Python server"
        echo "  status  - Show server status"
        echo "  logs    - Show recent logs"
        echo "  tail    - Follow logs in real-time"
        echo "  test    - Test API response"
        echo ""
        echo "Server URL: http://localhost:$PYTHON_PORT/api/status"
        ;;
esac 