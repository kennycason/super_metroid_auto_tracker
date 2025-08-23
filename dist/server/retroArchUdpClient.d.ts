import type { ConnectionInfo } from './types';
/**
 * RetroArch UDP client for communicating with RetroArch
 * Equivalent to the Kotlin RetroArchUDPClient
 */
export declare class RetroArchUDPClient {
    private host;
    private port;
    private timeoutMs;
    constructor(host?: string, port?: number);
    /**
     * Send a command to RetroArch and get the response
     */
    sendCommand(command: string): Promise<string | null>;
    /**
     * Read a range of memory from RetroArch
     */
    readMemoryRange(startAddress: number, size: number): Promise<Uint8Array | null>;
    /**
     * Get information about RetroArch and the loaded game
     */
    getRetroArchInfo(): Promise<ConnectionInfo>;
}
//# sourceMappingURL=retroArchUdpClient.d.ts.map