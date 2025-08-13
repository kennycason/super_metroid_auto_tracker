/**
 * RetroArch Backend Implementation
 * 
 * Wraps the existing RetroArchUDPClient to implement the EmulatorBackend interface.
 * This maintains backward compatibility while providing the new abstraction layer.
 */

import { RetroArchUDPClient } from './retroArchUdpClient';
import type { EmulatorBackend } from './emulatorBackend';
import type { ConnectionInfo } from './types';

/**
 * RetroArch backend implementation using UDP Network Commands (NWA)
 */
export class RetroArchBackend implements EmulatorBackend {
  private udpClient: RetroArchUDPClient;
  private host: string;
  private port: number;

  constructor(host: string = 'localhost', port: number = 55355) {
    this.host = host;
    this.port = port;
    this.udpClient = new RetroArchUDPClient(host, port);
  }

  /**
   * Connect to RetroArch (UDP is connectionless, so this just validates connectivity)
   */
  async connect(): Promise<boolean> {
    console.log(`🔌 RetroArch Backend: Attempting to connect to ${this.host}:${this.port}`);
    
    try {
      const connectionInfo = await this.udpClient.getRetroArchInfo();
      const connected = connectionInfo.connected;
      
      if (connected) {
        console.log(`✅ RetroArch Backend: Successfully connected to RetroArch`);
        console.log(`🔌 RetroArch Backend: Version: ${connectionInfo.retroarchVersion}`);
        console.log(`🔌 RetroArch Backend: Game loaded: ${connectionInfo.gameLoaded}`);
      } else {
        console.log(`❌ RetroArch Backend: Failed to connect to RetroArch`);
      }
      
      return connected;
    } catch (error) {
      console.log(`❌ RetroArch Backend: Connection error: ${error}`);
      return false;
    }
  }

  /**
   * Check if connected to RetroArch
   */
  async isConnected(): Promise<boolean> {
    try {
      const connectionInfo = await this.udpClient.getRetroArchInfo();
      return connectionInfo.connected;
    } catch (error) {
      console.log(`❌ RetroArch Backend: Connection check failed: ${error}`);
      return false;
    }
  }

  /**
   * Read memory range from RetroArch
   */
  async readMemoryRange(address: number, size: number): Promise<Uint8Array | null> {
    console.log(`🔌 RetroArch Backend: Reading memory 0x${address.toString(16).toUpperCase()} (${size} bytes)`);
    
    try {
      const result = await this.udpClient.readMemoryRange(address, size);
      if (result) {
        console.log(`✅ RetroArch Backend: Successfully read ${result.length} bytes from 0x${address.toString(16).toUpperCase()}`);
      } else {
        console.log(`❌ RetroArch Backend: Failed to read memory from 0x${address.toString(16).toUpperCase()}`);
      }
      return result;
    } catch (error) {
      console.log(`❌ RetroArch Backend: Memory read error: ${error}`);
      return null;
    }
  }

  /**
   * Get connection and game information
   */
  async getConnectionInfo(): Promise<ConnectionInfo> {
    console.log(`🔌 RetroArch Backend: Getting connection info`);
    
    try {
      const info = await this.udpClient.getRetroArchInfo();
      console.log(`✅ RetroArch Backend: Connection info retrieved - Connected: ${info.connected}, Game loaded: ${info.gameLoaded}`);
      return info;
    } catch (error) {
      console.log(`❌ RetroArch Backend: Failed to get connection info: ${error}`);
      return {
        connected: false,
        gameLoaded: false,
        retroarchVersion: 'Unknown',
        gameInfo: 'Connection failed'
      };
    }
  }

  /**
   * Get backend type identifier
   */
  getBackendType(): string {
    return 'retroarch';
  }

  /**
   * Disconnect from RetroArch (cleanup - UDP is connectionless so this is mostly a no-op)
   */
  async disconnect(): Promise<void> {
    console.log(`🔌 RetroArch Backend: Disconnecting (UDP is connectionless)`);
    // UDP is connectionless, so no actual disconnection needed
    // This method exists for interface compliance and future extensibility
  }

  /**
   * Get the current host and port configuration
   */
  getConfig(): { host: string; port: number } {
    return {
      host: this.host,
      port: this.port
    };
  }
}