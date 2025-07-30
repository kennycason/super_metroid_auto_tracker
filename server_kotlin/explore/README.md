# Super Metroid HUD Kotlin Server API Testing

This directory contains scripts for testing the Super Metroid HUD Kotlin server API.

## Changes Made to Fix Caching Issue

The following changes were made to fix the caching issue:

1. **BackgroundPoller.kt**:
   - Switched from MockRetroArchUDPClient to real RetroArchUDPClient
   - Enhanced resetCache() method to:
     - Reset bootstrapAttempted flag
     - Reset Mother Brain cache in the parser
     - Provide better logging

2. **HttpServer.kt**:
   - Added a /api/reset endpoint that calls poller.resetCache()
   - This ensures compatibility with the user's existing calls to /api/reset

## Test Scripts

### Bash Script (test_api.sh)

A simple bash script that tests the API endpoints using curl.

```bash
chmod +x test_api.sh
./test_api.sh
```

### Python Script (test_api.py)

A more comprehensive Python script that tests the API endpoints and displays the results in a readable format.

```bash
pip install requests
python test_api.py
```

## API Endpoints

- **/api/status**: Get the current server status and game state
- **/api/reset**: Reset the cache (new endpoint)
- **/api/reset-cache**: Reset the cache (original endpoint)
- **/api/reset-mb-cache**: Reset only the Mother Brain cache
- **/api/stats**: Get only the game stats
- **/game_state**: Get only the game state

## Testing Instructions

1. Start the server:
   ```bash
   cd ..
   bash manage_server.sh start
   ```

2. Run the test scripts:
   ```bash
   cd explore
   ./test_api.sh
   python test_api.py
   ```

3. Verify that the cache is properly reset by checking the output of the test scripts.

4. Stop the server:
   ```bash
   cd ..
   bash manage_server.sh stop
   ```

## Troubleshooting

If you encounter any issues:

1. Check the server logs:
   ```bash
   cd ..
   bash manage_server.sh logs
   ```

2. Make sure RetroArch is running and the Super Metroid ROM is loaded.

3. Try restarting the server:
   ```bash
   cd ..
   bash manage_server.sh restart
   ```
