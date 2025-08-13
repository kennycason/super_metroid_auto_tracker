/**
 * Browser-compatible RetroArch client
 * 
 * NOTE: Browsers cannot directly communicate with RetroArch via UDP due to security restrictions.
 * This implementation provides a fallback approach using a minimal WebSocket proxy.
 * 
 * For a truly backend-free solution, we would need:
 * 1. A browser extension with native messaging
 * 2. A desktop app that bridges WebSocket to UDP
 * 3. RetroArch to support WebSocket connections directly
 * 
 * Current implementation: Uses a minimal WebSocket server that proxies UDP to RetroArch
 */

export interface ConnectionInfo {
  connected: boolean;
  gameLoaded: boolean;
  retroarchVersion?: string;
  gameInfo?: string;
}

export interface MemoryData {
  [key: string]: Uint8Array | null;
}

/**
 * Browser-compatible RetroArch client using WebSocket proxy
 */
export class BrowserRetroArchClient {
  private ws: WebSocket | null = null;
  private host: string;
  private port: number;
  private timeoutMs: number;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 5;

  constructor(host: string = 'localhost', port: number = 8081) {
    this.host = host;
    this.port = port;
    this.timeoutMs = 1000;
  }

  /**
   * Connect to the WebSocket proxy server
   */
  async connect(): Promise<boolean> {
    return new Promise((resolve) => {
      try {
        this.ws = new WebSocket(`ws://${this.host}:${this.port}`);

        this.ws.onopen = () => {
          console.log('🔌 WebSocket: Connected to RetroArch proxy');
          this.reconnectAttempts = 0;
          resolve(true);
        };

        this.ws.onerror = (error) => {
          console.log('❌ WebSocket: Connection error:', error);
          resolve(false);
        };

        this.ws.onclose = () => {
          console.log('🔌 WebSocket: Connection closed');
          this.handleReconnect();
        };

        // Timeout for connection
        setTimeout(() => {
          if (this.ws?.readyState !== WebSocket.OPEN) {
            this.ws?.close();
            resolve(false);
          }
        }, this.timeoutMs);

      } catch (error) {
        console.log('❌ WebSocket: Failed to create connection:', error);
        resolve(false);
      }
    });
  }

  /**
   * Handle reconnection logic
   */
  private handleReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`🔄 WebSocket: Attempting reconnect ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
      setTimeout(() => this.connect(), 2000 * this.reconnectAttempts);
    }
  }

  /**
   * Send a command to RetroArch via WebSocket proxy
   */
  async sendCommand(command: string): Promise<string | null> {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.log('❌ WebSocket: Not connected, attempting to connect...');
      const connected = await this.connect();
      if (!connected) {
        return null;
      }
    }

    return new Promise((resolve) => {
      const requestId = Math.random().toString(36).substr(2, 9);
      let timeoutId: NodeJS.Timeout;

      const messageHandler = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          if (data.requestId === requestId) {
            clearTimeout(timeoutId);
            this.ws?.removeEventListener('message', messageHandler);
            resolve(data.response);
          }
        } catch (error) {
          console.log('❌ WebSocket: Failed to parse response:', error);
        }
      };

      // Set up timeout
      timeoutId = setTimeout(() => {
        this.ws?.removeEventListener('message', messageHandler);
        console.log('⏰ WebSocket: Request timeout');
        resolve(null);
      }, this.timeoutMs);

      // Listen for response
      this.ws?.addEventListener('message', messageHandler);

      // Send command
      const message = {
        requestId,
        command,
        type: 'udp_command'
      };

      try {
        this.ws?.send(JSON.stringify(message));
        console.log(`🔌 WebSocket: Sent command '${command}'`);
      } catch (error) {
        clearTimeout(timeoutId);
        this.ws?.removeEventListener('message', messageHandler);
        console.log(`❌ WebSocket: Failed to send command: ${error}`);
        resolve(null);
      }
    });
  }

  /**
   * Read a range of memory from RetroArch
   */
  async readMemoryRange(startAddress: number, size: number): Promise<Uint8Array | null> {
    const command = `READ_CORE_MEMORY 0x${startAddress.toString(16).toUpperCase()} ${size}`;
    const response = await this.sendCommand(command);

    if (!response) {
      return null;
    }

    if (!response.startsWith('READ_CORE_MEMORY')) {
      console.log(`❌ Invalid response format: ${response}`);
      return null;
    }

    try {
      const parts = response.split(' ', 3);
      if (parts.length < 3) {
        return null;
      }

      const hexData = parts[2].replace(/\s/g, '');
      if (hexData.length === 0) {
        return null;
      }

      // Convert hex string to Uint8Array
      const byteArray = new Uint8Array(hexData.length / 2);
      for (let i = 0; i < hexData.length; i += 2) {
        byteArray[i / 2] = parseInt(hexData.substr(i, 2), 16);
      }

      return byteArray;
    } catch (error) {
      console.log(`❌ Failed to parse memory data: ${error}`);
      return null;
    }
  }

  /**
   * Get information about RetroArch and the loaded game
   */
  async getRetroArchInfo(): Promise<ConnectionInfo> {
    const versionResponse = await this.sendCommand('VERSION');
    const gameInfoResponse = await this.sendCommand('GET_STATUS');

    const connected = versionResponse !== null;
    const gameLoaded = gameInfoResponse?.includes('PLAYING') === true;

    return {
      connected,
      retroarchVersion: versionResponse || 'Unknown',
      gameLoaded,
      gameInfo: gameInfoResponse || 'No game loaded'
    };
  }

  /**
   * Disconnect from the WebSocket proxy
   */
  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

/**
 * Fallback client that shows connection instructions
 */
export class MockRetroArchClient {
  async connect(): Promise<boolean> {
    console.log('🔌 Mock: Using mock RetroArch client (no real connection)');
    return false;
  }

  async sendCommand(_command: string): Promise<string | null> {
    return null;
  }

  async readMemoryRange(_startAddress: number, _size: number): Promise<Uint8Array | null> {
    return null;
  }

  async getRetroArchInfo(): Promise<ConnectionInfo> {
    return {
      connected: false,
      gameLoaded: false,
      retroarchVersion: 'Mock Client - No Connection',
      gameInfo: 'No game loaded (mock mode)'
    };
  }

  disconnect(): void {
    // No-op for mock client
  }
}
