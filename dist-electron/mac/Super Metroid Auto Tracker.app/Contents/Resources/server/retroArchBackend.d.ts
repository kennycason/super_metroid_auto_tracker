/**
 * RetroArch Backend Implementation
 *
 * Wraps the existing RetroArchUDPClient to implement the EmulatorBackend interface.
 * This maintains backward compatibility while providing the new abstraction layer.
 */
import type { EmulatorBackend } from './emulatorBackend';
import type { ConnectionInfo } from './types';
/**
 * RetroArch backend implementation using UDP Network Commands (NWA)
 */
export declare class RetroArchBackend implements EmulatorBackend {
    private udpClient;
    private host;
    private port;
    constructor(host?: string, port?: number);
    /**
     * Connect to RetroArch (UDP is connectionless, so this just validates connectivity)
     */
    connect(): Promise<boolean>;
    /**
     * Check if connected to RetroArch
     */
    isConnected(): Promise<boolean>;
    /**
     * Read memory range from RetroArch
     */
    readMemoryRange(address: number, size: number): Promise<Uint8Array | null>;
    /**
     * Get connection and game information
     */
    getConnectionInfo(): Promise<ConnectionInfo>;
    /**
     * Get backend type identifier
     */
    getBackendType(): string;
    /**
     * Disconnect from RetroArch (cleanup - UDP is connectionless so this is mostly a no-op)
     */
    disconnect(): Promise<void>;
    /**
     * Get the current host and port configuration
     */
    getConfig(): {
        host: string;
        port: number;
    };
}
//# sourceMappingURL=retroArchBackend.d.ts.map