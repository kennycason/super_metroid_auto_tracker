#!/bin/bash

# Super Metroid Tracker - Kotlin Native Build Script

set -e

echo "🎮 Super Metroid Tracker - Kotlin Native Backend"
echo "================================================="

# Check if we have Gradle
if command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
elif [ -f "./gradlew" ] && [ -x "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    echo "❌ Gradle not found. Please install Gradle or run: gradle wrapper --gradle-version 8.5"
    exit 1
fi

# Parse command line arguments
COMMAND=${1:-"build"}
PORT=${2:-"8080"}
POLL_INTERVAL=${3:-"1000"}

case $COMMAND in
    "build")
        echo "🔨 Building Kotlin Native binary..."
        $GRADLE_CMD build
        echo "✅ Build complete!"
        ;;
    "run")
        echo "🚀 Building and running server on port $PORT with $POLL_INTERVAL ms polling..."
        $GRADLE_CMD build
        echo "🌟 Starting server..."
        $GRADLE_CMD runDebugExecutableNative --args="$PORT $POLL_INTERVAL"
        ;;
    "test")
        echo "🧪 Running unit tests..."
        $GRADLE_CMD test
        echo "✅ Tests complete!"
        ;;
    "clean")
        echo "🧹 Cleaning build artifacts..."
        $GRADLE_CMD clean
        echo "✅ Clean complete!"
        ;;
    "release")
        echo "📦 Building release binary..."
        $GRADLE_CMD linkReleaseExecutableNative
        echo "✅ Release binary created in build/bin/native/releaseExecutable/"
        ;;
    *)
        echo "Usage: $0 [build|run|test|clean|release] [port] [poll_interval_ms]"
        echo ""
        echo "Commands:"
        echo "  build    - Build debug binary (default)"
        echo "  run      - Build and run server"
        echo "  test     - Run unit tests"
        echo "  clean    - Clean build artifacts"
        echo "  release  - Build optimized release binary"
        echo ""
        echo "Examples:"
        echo "  $0 build"
        echo "  $0 run 8080 1000"
        echo "  $0 test"
        exit 1
        ;;
esac 