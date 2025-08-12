#!/usr/bin/env python3
"""
Comprehensive test script for Super Metroid HUD Kotlin server API
This script tests the API endpoints and verifies that the cache is properly reset
"""

import requests
import json
import time
import sys

SERVER_URL = "http://localhost:8081"

def print_separator():
    print("=" * 60)

def call_api(endpoint, method="GET", data=None):
    """Make an API call and return the response"""
    url = f"{SERVER_URL}{endpoint}"
    print(f"Calling {method} {url}...")

    try:
        if method == "GET":
            response = requests.get(url)
        elif method == "POST":
            response = requests.post(url, json=data)
        else:
            print(f"Unsupported method: {method}")
            return None

        if response.status_code == 200:
            try:
                return response.json()
            except json.JSONDecodeError:
                return response.text
        else:
            print(f"Error: {response.status_code} - {response.text}")
            return None
    except Exception as e:
        print(f"Exception: {e}")
        return None

def print_game_state(state):
    """Print the game state in a readable format"""
    if not state:
        print("No game state available")
        return

    if isinstance(state, str):
        print(state)
        return

    # If we have a full server status, extract the stats
    if "stats" in state:
        stats = state["stats"]
    else:
        stats = state

    print(f"Health: {stats.get('health', 'N/A')}/{stats.get('max_health', 'N/A')}")
    print(f"Missiles: {stats.get('missiles', 'N/A')}/{stats.get('max_missiles', 'N/A')}")
    print(f"Supers: {stats.get('supers', 'N/A')}/{stats.get('max_supers', 'N/A')}")
    print(f"Power Bombs: {stats.get('power_bombs', 'N/A')}/{stats.get('max_power_bombs', 'N/A')}")
    print(f"Reserve Energy: {stats.get('reserve_energy', 'N/A')}/{stats.get('max_reserve_energy', 'N/A')}")
    print(f"Room ID: {stats.get('room_id', 'N/A')}")
    print(f"Area: {stats.get('area_name', 'N/A')} (ID: {stats.get('area_id', 'N/A')})")

    # Print items
    if "items" in stats:
        print("\nItems:")
        for item, value in stats["items"].items():
            print(f"  {item}: {value}")

    # Print beams
    if "beams" in stats:
        print("\nBeams:")
        for beam, value in stats["beams"].items():
            print(f"  {beam}: {value}")

    # Print bosses
    if "bosses" in stats:
        print("\nBosses:")
        for boss, value in stats["bosses"].items():
            print(f"  {boss}: {value}")

def main():
    print_separator()
    print("Super Metroid HUD Kotlin Server API Test")
    print(f"Server URL: {SERVER_URL}")
    print_separator()

    # Test 1: Get initial status
    print("\nTest 1: Get initial status")
    status = call_api("/api/status")
    if status:
        print("\nInitial game state:")
        print_game_state(status)

    # Test 2: Reset cache using /api/reset
    print_separator()
    print("\nTest 2: Reset cache using /api/reset")
    reset_result = call_api("/api/reset")
    print(f"Reset result: {reset_result}")

    # Test 3: Get status after reset
    print_separator()
    print("\nTest 3: Get status after reset")
    time.sleep(1)  # Give the server time to update
    status_after_reset = call_api("/api/status")
    if status_after_reset:
        print("\nGame state after reset:")
        print_game_state(status_after_reset)

    # Test 4: Reset cache using /api/reset-cache
    print_separator()
    print("\nTest 4: Reset cache using /api/reset-cache")
    reset_cache_result = call_api("/api/reset-cache", method="POST")
    print(f"Reset cache result: {reset_cache_result}")

    # Test 5: Get status after reset-cache
    print_separator()
    print("\nTest 5: Get status after reset-cache")
    time.sleep(1)  # Give the server time to update
    status_after_reset_cache = call_api("/api/status")
    if status_after_reset_cache:
        print("\nGame state after reset-cache:")
        print_game_state(status_after_reset_cache)

    # Test 6: Reset MB cache
    print_separator()
    print("\nTest 6: Reset MB cache")
    reset_mb_result = call_api("/api/reset-mb-cache")
    print(f"Reset MB cache result: {reset_mb_result}")

    print_separator()
    print("\nTests completed")

if __name__ == "__main__":
    main()
