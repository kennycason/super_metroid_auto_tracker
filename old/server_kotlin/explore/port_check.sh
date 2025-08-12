#!/bin/bash

# Port Check Script for Super Metroid HUD Kotlin Server
# This script helps diagnose and fix port conflicts

# Default port to check
PORT=8081

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -p|--port)
      PORT="$2"
      shift 2
      ;;
    -k|--kill)
      KILL_PROCESS=true
      shift
      ;;
    -f|--force)
      FORCE_KILL=true
      shift
      ;;
    -h|--help)
      echo "Port Check Script for Super Metroid HUD Kotlin Server"
      echo ""
      echo "Usage: $0 [options]"
      echo ""
      echo "Options:"
      echo "  -p, --port PORT    Port to check (default: 8081)"
      echo "  -k, --kill         Kill processes using the port"
      echo "  -f, --force        Force kill processes (kill -9)"
      echo "  -h, --help         Show this help message"
      echo ""
      echo "Examples:"
      echo "  $0 -p 8081         Check if port 8081 is in use"
      echo "  $0 -k              Kill processes using port 8081"
      echo "  $0 -p 8081 -k -f   Force kill processes using port 8081"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use -h or --help for usage information"
      exit 1
      ;;
  esac
done

echo "🔍 Checking port $PORT..."

# Check if port is in use
if lsof -ti:$PORT > /dev/null 2>&1; then
    echo "⚠️  Port $PORT is in use by the following process(es):"
    lsof -i:$PORT

    # Get PIDs of processes using the port
    PIDS=$(lsof -ti:$PORT)

    # Check if we should kill the processes
    if [[ "$KILL_PROCESS" == "true" ]]; then
        if [[ "$FORCE_KILL" == "true" ]]; then
            echo "🔥 Force killing process(es) using port $PORT..."
            for PID in $PIDS; do
                echo "   Killing PID $PID..."
                kill -9 $PID
            done
        else
            echo "🛑 Killing process(es) using port $PORT..."
            for PID in $PIDS; do
                echo "   Killing PID $PID..."
                kill $PID
            done
        fi

        # Wait a moment for processes to terminate
        sleep 2

        # Check if port is now available
        if lsof -ti:$PORT > /dev/null 2>&1; then
            echo "❌ Port $PORT is still in use after kill attempt."
            echo "   Try using -f to force kill, or manually kill the process:"
            echo "   sudo kill -9 $(lsof -ti:$PORT)"
        else
            echo "✅ Port $PORT is now available."
        fi
    else
        echo ""
        echo "To kill these processes, you can:"
        echo "1. Run this script with the -k option: $0 -k"
        echo "2. Force kill with: $0 -k -f"
        echo "3. Manually kill with: kill $(lsof -ti:$PORT)"
        echo "4. Force kill manually with: kill -9 $(lsof -ti:$PORT)"
        echo "5. Use sudo if needed: sudo kill -9 $(lsof -ti:$PORT)"
    fi
else
    echo "✅ Port $PORT is available."
fi

# Check for TIME_WAIT sockets
if netstat -an | grep -q "$PORT.*TIME_WAIT"; then
    echo ""
    echo "⚠️  Port $PORT has sockets in TIME_WAIT state:"
    netstat -an | grep "$PORT.*TIME_WAIT"
    echo ""
    echo "These sockets will be released automatically after the TIME_WAIT period (usually 1-2 minutes)."
    echo "If you need to use the port immediately, you can try changing the port in manage_server.sh."
fi

# Check for server_kotlin.kexe processes
if pgrep -f "server_kotlin.kexe" > /dev/null; then
    echo ""
    echo "⚠️  Found running server_kotlin.kexe processes:"
    ps -ef | grep "server_kotlin.kexe" | grep -v grep

    if [[ "$KILL_PROCESS" == "true" ]]; then
        if [[ "$FORCE_KILL" == "true" ]]; then
            echo "🔥 Force killing server_kotlin.kexe processes..."
            pkill -9 -f "server_kotlin.kexe"
        else
            echo "🛑 Killing server_kotlin.kexe processes..."
            pkill -f "server_kotlin.kexe"
        fi

        # Wait a moment for processes to terminate
        sleep 2

        # Check if processes are still running
        if pgrep -f "server_kotlin.kexe" > /dev/null; then
            echo "❌ server_kotlin.kexe processes are still running after kill attempt."
            echo "   Try using -f to force kill, or manually kill the processes:"
            echo "   pkill -9 -f \"server_kotlin.kexe\""
        else
            echo "✅ All server_kotlin.kexe processes have been terminated."
        fi
    else
        echo ""
        echo "To kill these processes, you can:"
        echo "1. Run this script with the -k option: $0 -k"
        echo "2. Force kill with: $0 -k -f"
        echo "3. Manually kill with: pkill -f \"server_kotlin.kexe\""
        echo "4. Force kill manually with: pkill -9 -f \"server_kotlin.kexe\""
    fi
fi

echo ""
echo "📋 Troubleshooting Tips:"
echo "1. If the server won't start due to port conflicts, try:"
echo "   - Stopping the server: ./manage_server.sh stop"
echo "   - Checking port status: ./explore/port_check.sh"
echo "   - Killing processes using the port: ./explore/port_check.sh -k"
echo "   - Force killing if necessary: ./explore/port_check.sh -k -f"
echo "2. If the issue persists, try changing the port in manage_server.sh"
echo "3. Check the server logs: ./manage_server.sh logs"
echo "4. Restart your system if all else fails"
