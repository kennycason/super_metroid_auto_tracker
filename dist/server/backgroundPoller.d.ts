import type { EmulatorBackend } from './emulatorBackend';
import type { ServerStatus } from './types';
/**
 * Background game state poller
 * Continuously polls RetroArch and maintains cached game state
 * Equivalent to the Kotlin BackgroundPoller
 */
export declare class BackgroundPoller {
    private backend;
    private parser;
    private updateInterval;
    private cache;
    private bootstrapAttempted;
    private pollingInterval;
    private isPolling;
    constructor(backend: EmulatorBackend, updateInterval?: number);
    /**
     * Start the background polling
     */
    start(): Promise<void>;
    /**
     * Stop the background polling
     */
    stop(): void;
    /**
     * Get the cached server status
     */
    getCachedState(): ServerStatus;
    /**
     * Reset the cache
     */
    resetCache(): void;
    /**
     * Main polling loop
     */
    private pollLoop;
    /**
     * Read game state from emulator
     */
    private readGameState;
    /**
     * Bootstrap Mother Brain cache if needed
     */
    private bootstrapMbCacheIfNeeded;
    /**
     * Get empty server status
     */
    private getEmptyServerStatus;
    /**
     * Get empty game state
     */
    private getEmptyGameState;
}
//# sourceMappingURL=backgroundPoller.d.ts.map