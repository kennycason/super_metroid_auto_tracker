"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.RetroArchUDPClient = void 0;
const dgram = __importStar(require("dgram"));
/**
 * RetroArch UDP client for communicating with RetroArch
 * Equivalent to the Kotlin RetroArchUDPClient
 */
class RetroArchUDPClient {
    host;
    port;
    timeoutMs;
    constructor(host = 'localhost', port = 55355) {
        this.host = host;
        this.port = port;
        this.timeoutMs = 1000; // 1 second timeout
    }
    /**
     * Send a command to RetroArch and get the response
     */
    async sendCommand(command) {
        console.log(`🔌 UDP: Sending command '${command}' to ${this.host}:${this.port}`);
        return new Promise((resolve) => {
            const client = dgram.createSocket('udp4');
            let timeoutId;
            // Set up timeout
            timeoutId = setTimeout(() => {
                console.log('⏰ UDP: Receive timeout');
                client.close();
                resolve(null);
            }, this.timeoutMs);
            // Handle response
            client.on('message', (msg) => {
                clearTimeout(timeoutId);
                const response = msg.toString().trim();
                console.log(`🔌 UDP: Received ${msg.length} bytes. Response: ${response}`);
                client.close();
                resolve(response);
            });
            // Handle errors
            client.on('error', (err) => {
                clearTimeout(timeoutId);
                console.log(`❌ UDP: Error for command ${command}: ${err.message}`);
                client.close();
                resolve(null);
            });
            // Send command
            try {
                const commandBuffer = Buffer.from(command);
                client.send(commandBuffer, this.port, this.host, (err) => {
                    if (err) {
                        clearTimeout(timeoutId);
                        console.log(`❌ UDP: Failed to send command: ${err.message}`);
                        client.close();
                        resolve(null);
                    }
                    else {
                        console.log(`🔌 UDP: Successfully sent ${commandBuffer.length} bytes`);
                    }
                });
            }
            catch (err) {
                clearTimeout(timeoutId);
                console.log(`❌ UDP: Error sending command: ${err}`);
                client.close();
                resolve(null);
            }
        });
    }
    /**
     * Read a range of memory from RetroArch
     */
    async readMemoryRange(startAddress, size) {
        console.log(`🔌 UDP: Reading memory range 0x${startAddress.toString(16).toUpperCase()} (size: ${size})`);
        const command = `READ_CORE_MEMORY 0x${startAddress.toString(16).toUpperCase()} ${size}`;
        console.log(`🔌 UDP: Sending memory read command: ${command}`);
        const response = await this.sendCommand(command);
        if (!response) {
            console.log('❌ UDP: Failed to read memory range - no response received');
            return null;
        }
        if (!response.startsWith('READ_CORE_MEMORY')) {
            console.log(`❌ UDP: Invalid response format: ${response}`);
            return null;
        }
        try {
            console.log('🔌 UDP: Parsing memory data from response...');
            const parts = response.split(' ');
            if (parts.length < 3) {
                console.log(`❌ UDP: Invalid response format (not enough parts): ${response}`);
                return null;
            }
            // Extract all hex data after the address (parts[2] onwards)
            const hexParts = parts.slice(2);
            const hexData = hexParts.join('');
            console.log(`🔌 UDP: Hex data: ${hexData}`);
            if (hexData.length === 0) {
                console.log('❌ UDP: Empty hex data in response');
                return null;
            }
            // Convert hex string to Uint8Array
            const byteArray = new Uint8Array(hexData.length / 2);
            for (let i = 0; i < hexData.length; i += 2) {
                byteArray[i / 2] = parseInt(hexData.substr(i, 2), 16);
            }
            console.log(`🔌 UDP: Successfully parsed ${byteArray.length} bytes of memory data`);
            return byteArray;
        }
        catch (err) {
            console.log(`❌ UDP: Failed to parse memory data: ${err}`);
            return null;
        }
    }
    /**
     * Get information about RetroArch and the loaded game
     */
    async getRetroArchInfo() {
        console.log('🔌 UDP: Getting RetroArch info...');
        console.log('🔌 UDP: Checking RetroArch version...');
        const versionResponse = await this.sendCommand('VERSION');
        console.log(`🔌 UDP: Version response: ${versionResponse}`);
        console.log('🔌 UDP: Checking game status...');
        const gameInfoResponse = await this.sendCommand('GET_STATUS');
        console.log(`🔌 UDP: Game info response: ${gameInfoResponse}`);
        const connected = versionResponse !== null;
        const gameLoaded = gameInfoResponse?.includes('PLAYING') === true;
        console.log('🔌 UDP: RetroArch info summary:');
        console.log(`🔌 UDP: - Connected: ${connected}`);
        console.log(`🔌 UDP: - RetroArch version: ${versionResponse || 'Unknown'}`);
        console.log(`🔌 UDP: - Game loaded: ${gameLoaded}`);
        console.log(`🔌 UDP: - Game info: ${gameInfoResponse || 'No game loaded'}`);
        return {
            connected,
            retroarchVersion: versionResponse || 'Unknown',
            gameLoaded,
            gameInfo: gameInfoResponse || 'No game loaded'
        };
    }
}
exports.RetroArchUDPClient = RetroArchUDPClient;
//# sourceMappingURL=retroArchUdpClient.js.map