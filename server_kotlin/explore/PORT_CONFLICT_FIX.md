# Super Metroid HUD Kotlin Server Port Conflict Fix

## Issue Description

The Kotlin server was failing to start with the following error:

```
Uncaught Kotlin exception: io.ktor.utils.io.errors.PosixException.AddressAlreadyInUseException: EADDRINUSE (48): Address already in use
```

This error occurs when the server tries to bind to port 8081, but the port is already in use by another process. This can happen if:

1. A previous instance of the server wasn't properly shut down
2. Another application is using the same port
3. The port is in a TIME_WAIT state from a previous connection

## Root Causes

After examining the code, we identified the following issues:

1. **Incorrect Executable Name**: The `manage_server.sh` script was trying to kill processes named "server.kexe", but the actual executable is named "server_kotlin.kexe". This meant that previous instances of the server weren't being properly terminated.

2. **Insufficient Port Verification**: The script didn't properly verify that the port was free before starting the server.

3. **Inadequate Wait Time**: The script didn't wait long enough after killing processes for the port to be released.

## Changes Made

### 1. Updated manage_server.sh

The following changes were made to the `manage_server.sh` script:

- Updated pkill commands to use the correct executable name "server_kotlin.kexe"
- Added verification that the port is free before starting the server
- Increased the sleep time after killing processes
- Added more robust error handling
- Enhanced the status command to provide more detailed information

### 2. Created port_check.sh

Created a new script `port_check.sh` to help diagnose and fix port conflicts:

- Checks if port 8081 (or a user-specified port) is in use
- Identifies the processes using the port
- Provides options to kill the processes (normal or force kill)
- Checks for sockets in TIME_WAIT state
- Checks for running server_kotlin.kexe processes
- Provides detailed troubleshooting tips

## How to Use

### Starting the Server

```bash
./manage_server.sh start
```

The script will now:
1. Kill any existing server processes
2. Verify that port 8081 is available
3. Build and start the server
4. Verify that the server is running

### Checking Server Status

```bash
./manage_server.sh status
```

This will show:
- Whether the server is running
- The PID of the server process
- Whether the server is responding to health checks
- If the server is not running, whether port 8081 is in use by another process

### Stopping the Server

```bash
./manage_server.sh stop
```

This will:
1. Kill any server processes
2. Verify that the server has stopped

### Diagnosing Port Conflicts

If you're experiencing port conflicts, you can use the `port_check.sh` script:

```bash
./explore/port_check.sh
```

This will:
1. Check if port 8081 is in use
2. Identify the processes using the port
3. Check for sockets in TIME_WAIT state
4. Check for running server_kotlin.kexe processes
5. Provide troubleshooting tips

To kill processes using port 8081:

```bash
./explore/port_check.sh -k
```

To force kill processes:

```bash
./explore/port_check.sh -k -f
```

To check a different port:

```bash
./explore/port_check.sh -p 8082
```

## Troubleshooting

If the server still won't start:

1. **Check if port 8081 is in use**:
   ```bash
   ./explore/port_check.sh
   ```

2. **Kill any processes using port 8081**:
   ```bash
   ./explore/port_check.sh -k -f
   ```

3. **Check the server logs**:
   ```bash
   ./manage_server.sh logs
   ```

4. **Try changing the port**:
   Edit the `KOTLIN_PORT` variable in `manage_server.sh` to use a different port.

5. **Check for TIME_WAIT sockets**:
   ```bash
   netstat -an | grep 8081.*TIME_WAIT
   ```
   If there are many sockets in TIME_WAIT state, wait a few minutes for them to be released.

6. **Restart your system**:
   As a last resort, restart your system to clear all port bindings.

## Conclusion

These changes should fix the port conflict issue and make the server more robust. If you continue to experience issues, please check the logs and use the `port_check.sh` script to diagnose the problem.
