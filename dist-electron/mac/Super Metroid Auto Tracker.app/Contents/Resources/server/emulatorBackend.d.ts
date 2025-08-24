/**
 * Emulator Backend Interface
 *
 * Provides a common interface for different emulator backends (RetroArch, Mesen, etc.)
 * This allows the tracker to support multiple emulators without changing the core logic.
 */
import type { ConnectionInfo } from './types';
/**
 * Common interface that all emulator backends must implement
 */
export interface EmulatorBackend {
    /**
     * Connect to the emulator (if needed)
     * @returns Promise that resolves to true if connection successful
     */
    connect(): Promise<boolean>;
    /**
     * Check if currently connected to the emulator
     * @returns Promise that resolves to true if connected
     */
    isConnected(): Promise<boolean>;
    /**
     * Read a range of memory from the emulator
     * @param address Memory address to read from
     * @param size Number of bytes to read
     * @returns Promise that resolves to Uint8Array of memory data, or null if failed
     */
    readMemoryRange(address: number, size: number): Promise<Uint8Array | null>;
    /**
     * Get connection and game information from the emulator
     * @returns Promise that resolves to ConnectionInfo
     */
    getConnectionInfo(): Promise<ConnectionInfo>;
    /**
     * Get the backend type identifier
     * @returns String identifier for this backend type
     */
    getBackendType(): string;
    /**
     * Disconnect from the emulator (cleanup)
     * @returns Promise that resolves when disconnection is complete
     */
    disconnect(): Promise<void>;
}
/**
 * Configuration for different backend types
 */
export interface BackendConfig {
    type: 'retroarch' | 'mesen';
    retroarch?: {
        host: string;
        port: number;
    };
    mesen?: {
        host: string;
        httpPort: number;
        websocketPort?: number;
    };
}
/**
 * Default backend configurations
 */
export declare const DEFAULT_BACKEND_CONFIG: BackendConfig;
/**
 * Factory function to create the appropriate emulator backend
 * @param config Backend configuration
 * @returns EmulatorBackend instance
 */
export declare function createEmulatorBackend(config?: BackendConfig): Promise<EmulatorBackend>;
/**
 * Parse backend configuration from environment variables or command line arguments
 * @param args Command line arguments
 * @returns BackendConfig
 */
export declare function parseBackendConfig(args?: string[]): BackendConfig;
//# sourceMappingURL=emulatorBackend.d.ts.map