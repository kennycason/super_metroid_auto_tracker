#!/bin/bash

# Super Metroid Tracker Server Management
# Manages Python (8081) and Kotlin (8082) servers

PYTHON_PORT=8081
KOTLIN_PORT=8082
PYTHON_LOG="python-server.log"
KOTLIN_LOG="kotlin-server.log"

case "$1" in
    start)
        echo "🚀 Starting Super Metroid Tracker servers..."
        
        # Kill any existing processes
        pkill -f "background_poller_server.py" 2>/dev/null
        pkill -f "server.kexe" 2>/dev/null
        sleep 2
        
        # Start Python server
        echo "🐍 Starting Python server on port $PYTHON_PORT..."
        python3 background_poller_server.py > "$PYTHON_LOG" 2>&1 &
        PYTHON_PID=$!
        
        # Start Kotlin server  
        echo "⚡ Starting Kotlin server on port $KOTLIN_PORT..."
        ./server/build/bin/native/debugExecutable/server.kexe $KOTLIN_PORT > "$KOTLIN_LOG" 2>&1 &
        KOTLIN_PID=$!
        
        sleep 3
        
        # Test servers
        echo "🔍 Testing servers..."
        if curl -s http://localhost:$PYTHON_PORT/api/status > /dev/null; then
            echo "✅ Python server (port $PYTHON_PORT) - RUNNING"
        else
            echo "❌ Python server (port $PYTHON_PORT) - FAILED"
        fi
        
        if curl -s http://localhost:$KOTLIN_PORT/api/status > /dev/null; then
            echo "✅ Kotlin server (port $KOTLIN_PORT) - RUNNING"
        else
            echo "❌ Kotlin server (port $KOTLIN_PORT) - FAILED"
        fi
        
        echo "📝 Logs: $PYTHON_LOG, $KOTLIN_LOG"
        ;;
        
    stop)
        echo "🛑 Stopping all servers..."
        pkill -f "background_poller_server.py"
        pkill -f "server.kexe"
        echo "✅ All servers stopped"
        ;;
        
    status)
        echo "📊 Server Status:"
        if pgrep -f "background_poller_server.py" > /dev/null; then
            echo "🐍 Python server - RUNNING (port $PYTHON_PORT)"
        else
            echo "🐍 Python server - STOPPED"
        fi
        
        if pgrep -f "server.kexe" > /dev/null; then
            echo "⚡ Kotlin server - RUNNING (port $KOTLIN_PORT)"
        else
            echo "⚡ Kotlin server - STOPPED"
        fi
        ;;
        
    logs)
        echo "📋 Recent logs:"
        echo "=== PYTHON SERVER ==="
        tail -n 10 "$PYTHON_LOG" 2>/dev/null || echo "No Python logs found"
        echo -e "\n=== KOTLIN SERVER ==="
        tail -n 10 "$KOTLIN_LOG" 2>/dev/null || echo "No Kotlin logs found"
        ;;
        
    test)
        echo "🧪 Testing API responses..."
        echo "=== PYTHON SERVER (8081) ==="
        curl -s http://localhost:8081/api/status | jq -c '{connected, game_loaded, poll_count}'
        echo -e "\n=== KOTLIN SERVER (8082) ==="
        curl -s http://localhost:8082/api/status | jq -c '{connected, game_loaded, poll_count}'
        ;;
        
    *)
        echo "Super Metroid Tracker Server Management"
        echo ""
        echo "Usage: $0 {start|stop|status|logs|test}"
        echo ""
        echo "Commands:"
        echo "  start  - Start both Python (8081) and Kotlin (8082) servers"
        echo "  stop   - Stop all servers"
        echo "  status - Show server status"
        echo "  logs   - Show recent logs from both servers"
        echo "  test   - Test API responses from both servers"
        echo ""
        echo "Servers:"
        echo "  🐍 Python: http://localhost:8081/api/status"
        echo "  ⚡ Kotlin: http://localhost:8082/api/status"
        ;;
esac 