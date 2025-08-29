# Super Metroid Auto Tracker - Troubleshooting Guide

## Memory Operation Issues

If you encounter issues with memory operations, such as errors in the logs about "Invalid memory response format" or "Write operation returned unsuccessful response", the following fixes have been implemented:

1. **Improved Error Handling**: The application now properly handles mixed-up responses between read and write operations.
2. **Thread-Safe Operations**: Memory operations are now synchronized using a mutex to prevent race conditions.
3. **Enhanced Grayscale Effect**: The grayscale effect has been improved with better contrast and tone mapping.
4. **Connection Health Monitoring**: The application now periodically checks connection health and automatically recovers from temporary connection issues.
5. **Detailed Error Reporting**: Error messages are now displayed in the UI and include more detailed information about the cause of the error.
6. **Performance Tracking**: The application now tracks performance metrics such as write times and success rates to help identify bottlenecks.
7. **Fixed Control Flow**: Fixed "Unexpected code path in readMemory" errors by restructuring the memory read/write methods to ensure proper control flow and prevent execution from reaching unreachable code paths.

### Unexpected Code Path Errors

If you see errors like `IllegalStateException: Unexpected code path in readMemory` or `Unexpected code path in writeMemory` in your logs, these have been fixed by:

1. Restructuring the memory read/write methods to use a clearer return flow
2. Moving return statements outside nested blocks to ensure proper control flow
3. Simplifying the retry logic to be in one place
4. Removing the unreachable code paths that were causing the exceptions

These changes ensure that the application properly handles all memory operations without reaching unexpected code paths.

## Testing the Fixes

To verify that the fixes are working correctly:

1. Start RetroArch with Super Metroid loaded
2. Launch Super Metroid Auto Tracker with:
   ```
   ./gradlew build && ./gradlew run
   ```
3. Enable the "effects" panel by clicking the "effects" button at the bottom of the window
4. Try each effect type (Psychedelic, Neon, Rainbow, Grayscale) and verify they work correctly
5. Check the logs for any errors:
   ```
   cat ~/.smtracker/smtracker.log | grep -i "error\|fail\|exception"
   ```

## Troubleshooting Steps

If you still encounter issues:

### Connection Issues

1. **Verify RetroArch Settings**:
   - Ensure RetroArch is running with network commands enabled
   - Check that the UDP port (default: 55355) is not blocked by a firewall

2. **Check Connection Status**:
   - The application shows "Connected" or "Disconnected" at the bottom of the window
   - If it shows "Disconnected", try restarting RetroArch

### Effect Issues

1. **Grayscale Effect Not Working**:
   - Ensure Super Metroid is loaded and running
   - Try adjusting the intensity slider
   - Check if other effects are working

2. **Memory Operation Errors**:
   - Check the logs for specific error messages:
     ```
     cat ~/.smtracker/smtracker.log | grep -i "memory\|udp\|socket"
     ```
   - If you see "WRITE_CORE_MEMORY response during read operation", this is expected and handled

### Performance Issues

1. **Slow or Laggy Effects**:
   - The application limits the number of memory operations to prevent overloading
   - Try reducing the intensity of the effects
   - Check if RetroArch is running with high CPU usage

2. **Application Freezes**:
   - Check if RetroArch is still responding
   - Try stopping and restarting the effects

## Advanced Debugging

For advanced debugging:

1. **Enable Debug Logging**:
   - Edit `src/main/resources/logback.xml` to set the root level to "DEBUG"
   - Restart the application

2. **Monitor UDP Traffic**:
   - Use a tool like Wireshark to monitor UDP traffic on port 55355
   - Look for patterns of requests and responses

3. **Check Memory Addresses**:
   - Verify that the memory addresses being accessed are correct for your ROM
   - Different ROM versions may have slightly different memory layouts

## Reporting Issues

If you continue to experience issues:

1. Collect the logs:
   ```
   cat ~/.smtracker/smtracker.log > smtracker_issue.log
   ```

2. Note the steps to reproduce the issue

3. Include information about your environment:
   - Operating system
   - RetroArch version
   - Super Metroid ROM version

4. Submit an issue with this information
