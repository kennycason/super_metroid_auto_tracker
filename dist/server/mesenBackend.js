"use strict";
/**
 * Mesen Backend Implementation
 *
 * Implements the EmulatorBackend interface using Mesen 2.1.1's HTTP API.
 * Provides an alternative to RetroArch for Super Metroid tracking.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.MesenBackend = void 0;
/**
 * Mesen backend implementation using HTTP API
 */
class MesenBackend {
    host;
    httpPort;
    baseUrl;
    timeoutMs;
    constructor(host = 'localhost', httpPort = 8080) {
        this.host = host;
        this.httpPort = httpPort;
        this.baseUrl = `http://${host}:${httpPort}`;
        this.timeoutMs = 5000; // 5 second timeout for HTTP requests
    }
    /**
     * Connect to Mesen (validate HTTP API is accessible)
     */
    async connect() {
        console.log(`🔌 Mesen Backend: Attempting to connect to ${this.baseUrl}`);
        try {
            const response = await this.fetchWithTimeout(`${this.baseUrl}/api/state`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                }
            });
            if (response.ok) {
                const data = await response.json();
                console.log(`✅ Mesen Backend: Successfully connected to Mesen`);
                console.log(`🔌 Mesen Backend: State: ${JSON.stringify(data)}`);
                return true;
            }
            else {
                console.log(`❌ Mesen Backend: HTTP ${response.status} - ${response.statusText}`);
                return false;
            }
        }
        catch (error) {
            console.log(`❌ Mesen Backend: Connection error: ${error}`);
            return false;
        }
    }
    /**
     * Check if connected to Mesen
     */
    async isConnected() {
        try {
            const response = await this.fetchWithTimeout(`${this.baseUrl}/api/state`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                }
            });
            return response.ok;
        }
        catch (error) {
            console.log(`❌ Mesen Backend: Connection check failed: ${error}`);
            return false;
        }
    }
    /**
     * Read memory range from Mesen using HTTP API
     */
    async readMemoryRange(address, size) {
        console.log(`🔌 Mesen Backend: Reading memory 0x${address.toString(16).toUpperCase()} (${size} bytes)`);
        try {
            const url = `${this.baseUrl}/api/memory/read?address=0x${address.toString(16)}&length=${size}`;
            console.log(`🔌 Mesen Backend: HTTP GET ${url}`);
            const response = await this.fetchWithTimeout(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                }
            });
            if (!response.ok) {
                console.log(`❌ Mesen Backend: HTTP ${response.status} - ${response.statusText}`);
                return null;
            }
            const data = await response.json();
            console.log(`🔌 Mesen Backend: Received JSON response: ${JSON.stringify(data)}`);
            // Convert the response to Uint8Array
            // Mesen might return data in different formats, so we need to handle various cases
            let bytes = [];
            if (data.bytes && Array.isArray(data.bytes)) {
                // Format: { bytes: [0x12, 0x34, ...] }
                bytes = data.bytes;
            }
            else if (data.data && Array.isArray(data.data)) {
                // Format: { data: [0x12, 0x34, ...] }
                bytes = data.data;
            }
            else if (Array.isArray(data)) {
                // Format: [0x12, 0x34, ...]
                bytes = data;
            }
            else if (typeof data === 'string') {
                // Format: hex string "1234ABCD"
                const hexString = data.replace(/0x/g, '').replace(/\s/g, '');
                bytes = [];
                for (let i = 0; i < hexString.length; i += 2) {
                    bytes.push(parseInt(hexString.substr(i, 2), 16));
                }
            }
            else {
                console.log(`❌ Mesen Backend: Unknown response format: ${JSON.stringify(data)}`);
                return null;
            }
            const result = new Uint8Array(bytes);
            console.log(`✅ Mesen Backend: Successfully read ${result.length} bytes from 0x${address.toString(16).toUpperCase()}`);
            return result;
        }
        catch (error) {
            console.log(`❌ Mesen Backend: Memory read error: ${error}`);
            return null;
        }
    }
    /**
     * Get connection and game information from Mesen
     */
    async getConnectionInfo() {
        console.log(`🔌 Mesen Backend: Getting connection info`);
        try {
            // Get emulator state
            const stateResponse = await this.fetchWithTimeout(`${this.baseUrl}/api/state`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                }
            });
            if (!stateResponse.ok) {
                console.log(`❌ Mesen Backend: Failed to get state - HTTP ${stateResponse.status}`);
                return {
                    connected: false,
                    gameLoaded: false,
                    retroarchVersion: 'Mesen (connection failed)',
                    gameInfo: 'Connection failed'
                };
            }
            const stateData = await stateResponse.json();
            console.log(`🔌 Mesen Backend: State data: ${JSON.stringify(stateData)}`);
            // Get game information
            let gameInfo = 'No game loaded';
            let gameLoaded = false;
            try {
                const gameResponse = await this.fetchWithTimeout(`${this.baseUrl}/api/game`, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json',
                    }
                });
                if (gameResponse.ok) {
                    const gameData = await gameResponse.json();
                    console.log(`🔌 Mesen Backend: Game data: ${JSON.stringify(gameData)}`);
                    // Check if a game is loaded
                    gameLoaded = gameData && (gameData.loaded === true ||
                        gameData.running === true ||
                        gameData.name ||
                        gameData.title ||
                        stateData.running === true ||
                        stateData.loaded === true);
                    if (gameLoaded) {
                        gameInfo = gameData.name || gameData.title || 'Game loaded';
                    }
                }
            }
            catch (gameError) {
                console.log(`⚠️ Mesen Backend: Could not get game info: ${gameError}`);
                // Assume game is loaded if we can connect to the API
                gameLoaded = true;
                gameInfo = 'Game status unknown';
            }
            const connectionInfo = {
                connected: true,
                gameLoaded: gameLoaded,
                retroarchVersion: `Mesen 2.1.1 (HTTP API)`,
                gameInfo: gameInfo
            };
            console.log(`✅ Mesen Backend: Connection info retrieved - Connected: ${connectionInfo.connected}, Game loaded: ${connectionInfo.gameLoaded}`);
            return connectionInfo;
        }
        catch (error) {
            console.log(`❌ Mesen Backend: Failed to get connection info: ${error}`);
            return {
                connected: false,
                gameLoaded: false,
                retroarchVersion: 'Mesen (error)',
                gameInfo: 'Connection failed'
            };
        }
    }
    /**
     * Get backend type identifier
     */
    getBackendType() {
        return 'mesen';
    }
    /**
     * Disconnect from Mesen (cleanup)
     */
    async disconnect() {
        console.log(`🔌 Mesen Backend: Disconnecting (HTTP is stateless)`);
        // HTTP is stateless, so no actual disconnection needed
        // This method exists for interface compliance and future extensibility
    }
    /**
     * Get the current host and port configuration
     */
    getConfig() {
        return {
            host: this.host,
            httpPort: this.httpPort,
            baseUrl: this.baseUrl
        };
    }
    /**
     * Fetch with timeout support
     */
    async fetchWithTimeout(url, options) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), this.timeoutMs);
        try {
            const response = await fetch(url, {
                ...options,
                signal: controller.signal
            });
            clearTimeout(timeoutId);
            return response;
        }
        catch (error) {
            clearTimeout(timeoutId);
            throw error;
        }
    }
}
exports.MesenBackend = MesenBackend;
//# sourceMappingURL=mesenBackend.js.map