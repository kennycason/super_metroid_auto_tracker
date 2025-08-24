import type { BackendConfig } from './emulatorBackend';
/**
 * HTTP server serving cached game state data
 * Equivalent to the Kotlin HttpServer
 */
export declare class HttpServer {
    private app;
    private poller;
    private port;
    private pollInterval;
    private backendConfig;
    private server;
    private initialized;
    constructor(port?: number, pollInterval?: number, backendConfig?: BackendConfig);
    /**
     * Initialize the backend and poller (async initialization)
     */
    private initialize;
    /**
     * Start the HTTP server and background poller
     */
    start(): Promise<void>;
    /**
     * Stop the HTTP server and background poller
     */
    stop(): void;
    /**
     * Configure Express server middleware
     */
    private configureServer;
    private getStaticFilesPath;
    /**
     * Configure API routes
     */
    private configureRouting;
}
//# sourceMappingURL=httpServer.d.ts.map