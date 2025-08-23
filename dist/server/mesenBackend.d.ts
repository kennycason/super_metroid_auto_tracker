/**
 * Mesen Backend Implementation
 *
 * Implements the EmulatorBackend interface using Mesen 2.1.1's HTTP API.
 * Provides an alternative to RetroArch for Super Metroid tracking.
 */
import type { EmulatorBackend } from './emulatorBackend';
import type { ConnectionInfo } from './types';
/**
 * Mesen backend implementation using HTTP API
 */
export declare class MesenBackend implements EmulatorBackend {
    private host;
    private httpPort;
    private baseUrl;
    private timeoutMs;
    constructor(host?: string, httpPort?: number);
    /**
     * Connect to Mesen (validate HTTP API is accessible)
     */
    connect(): Promise<boolean>;
    /**
     * Check if connected to Mesen
     */
    isConnected(): Promise<boolean>;
    /**
     * Read memory range from Mesen using HTTP API
     */
    readMemoryRange(address: number, size: number): Promise<Uint8Array | null>;
    /**
     * Get connection and game information from Mesen
     */
    getConnectionInfo(): Promise<ConnectionInfo>;
    /**
     * Get backend type identifier
     */
    getBackendType(): string;
    /**
     * Disconnect from Mesen (cleanup)
     */
    disconnect(): Promise<void>;
    /**
     * Get the current host and port configuration
     */
    getConfig(): {
        host: string;
        httpPort: number;
        baseUrl: string;
    };
    /**
     * Fetch with timeout support
     */
    private fetchWithTimeout;
}
//# sourceMappingURL=mesenBackend.d.ts.map