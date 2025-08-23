"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.HttpServer = void 0;
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const path_1 = __importDefault(require("path"));
// __dirname is available in CommonJS builds (production)
// In development with tsx, we'll fallback to a sensible default if needed
const backgroundPoller_1 = require("./backgroundPoller");
const emulatorBackend_1 = require("./emulatorBackend");
// CommonJS __dirname is available in CommonJS modules
/**
 * HTTP server serving cached game state data
 * Equivalent to the Kotlin HttpServer
 */
class HttpServer {
    app;
    poller = null;
    port;
    pollInterval;
    backendConfig;
    server = null;
    initialized = false;
    constructor(port = 8000, pollInterval = 1000, backendConfig = emulatorBackend_1.DEFAULT_BACKEND_CONFIG) {
        this.port = port;
        this.pollInterval = pollInterval;
        this.backendConfig = backendConfig;
        this.app = (0, express_1.default)();
        this.configureServer();
        this.configureRouting();
    }
    /**
     * Initialize the backend and poller (async initialization)
     */
    async initialize() {
        if (this.initialized)
            return;
        console.log('🔧 HttpServer: Initializing backend...');
        // Create the appropriate backend using the factory
        const backend = await (0, emulatorBackend_1.createEmulatorBackend)(this.backendConfig);
        this.poller = new backgroundPoller_1.BackgroundPoller(backend, this.pollInterval);
        this.initialized = true;
        console.log('✅ HttpServer: Backend initialized successfully');
    }
    /**
     * Start the HTTP server and background poller
     */
    async start() {
        try {
            // Initialize backend and poller first
            await this.initialize();
            // Start background poller
            if (this.poller) {
                await this.poller.start();
            }
            else {
                throw new Error('Poller not initialized');
            }
            // Start HTTP server
            this.server = this.app.listen(this.port, () => {
                console.log('🚀 Background Polling Super Metroid Tracker Server');
                console.log('='.repeat(50));
                console.log(`📱 Tracker UI: http://localhost:${this.port}/`);
                console.log(`📊 API Status: http://localhost:${this.port}/api/status`);
                console.log(`📈 API Stats:  http://localhost:${this.port}/api/stats`);
                console.log(`⚡ Background polling: ${this.pollInterval}ms intervals`);
                console.log('🎯 Architecture: Background UDP + Instant Cache Serving');
                console.log('⏹️  Press Ctrl+C to stop');
                console.log('='.repeat(50));
            });
        }
        catch (err) {
            console.log(`Server error: ${err}`);
            this.stop();
        }
    }
    /**
     * Stop the HTTP server and background poller
     */
    stop() {
        if (this.poller) {
            this.poller.stop();
        }
        if (this.server) {
            this.server.close();
        }
        console.log('🏁 Server stopped');
    }
    /**
     * Configure Express server middleware
     */
    configureServer() {
        // Enable CORS for all routes
        this.app.use((0, cors_1.default)({
            origin: true,
            methods: ['GET', 'POST', 'PUT', 'DELETE'],
            allowedHeaders: ['Content-Type']
        }));
        // Parse JSON bodies
        this.app.use(express_1.default.json());
        // Serve static files from React build (if available)
        const buildPath = path_1.default.join(__dirname, '../../dist');
        this.app.use(express_1.default.static(buildPath));
    }
    /**
     * Configure API routes
     */
    configureRouting() {
        // Main endpoints
        this.app.get('/', (_req, res) => {
            // Serve React app if built, otherwise show simple HTML
            const buildPath = path_1.default.join(__dirname, '../../dist/index.html');
            try {
                res.sendFile(buildPath);
            }
            catch {
                res.send(`
          <!DOCTYPE html>
          <html>
          <head><title>Super Metroid Tracker (TypeScript)</title></head>
          <body>
              <h1>🎮 Super Metroid Tracker (TypeScript)</h1>
              <p>TypeScript backend server is running!</p>
              <ul>
                  <li><a href="/api/status">📊 Server Status</a></li>
                  <li><a href="/api/stats">📈 Game Stats</a></li>
                  <li><a href="/game_state">🎯 Game State</a></li>
              </ul>
              <p><em>React frontend should be available when built</em></p>
          </body>
          </html>
        `);
            }
        });
        this.app.get('/api/status', (_req, res) => {
            if (!this.poller) {
                res.status(503).json({ error: 'Server not initialized' });
                return;
            }
            const status = this.poller.getCachedState();
            res.json(status);
        });
        this.app.get('/api/stats', (_req, res) => {
            if (!this.poller) {
                res.status(503).json({ error: 'Server not initialized' });
                return;
            }
            const status = this.poller.getCachedState();
            res.json(status.stats);
        });
        this.app.get('/game_state', (_req, res) => {
            if (!this.poller) {
                res.status(503).json({ error: 'Server not initialized' });
                return;
            }
            const status = this.poller.getCachedState();
            res.json(status.stats);
        });
        this.app.post('/api/reset-cache', (_req, res) => {
            if (!this.poller) {
                res.status(503).json({ error: 'Server not initialized' });
                return;
            }
            this.poller.resetCache();
            res.json({ message: 'Cache reset successfully' });
        });
        this.app.get('/api/bootstrap-mb', (_req, res) => {
            res.json({ message: 'Bootstrap triggered' });
        });
        this.app.get('/api/manual-mb-complete', (_req, res) => {
            try {
                // Note: In full implementation, this would manually set MB completion
                const response = {
                    message: 'MB1 and MB2 manually set to completed',
                    mb1: true,
                    mb2: true
                };
                res.json(response);
                console.log('🔧 Manual MB completion triggered via API');
            }
            catch (err) {
                res.status(500).json({ error: String(err) });
            }
        });
        this.app.get('/api/reset-mb-cache', (_req, res) => {
            try {
                // Reset Mother Brain cache in parser (would need access to parser)
                const response = { message: 'MB cache reset to default (not detected)' };
                res.json(response);
                console.log('🔄 MB cache reset via API');
            }
            catch (err) {
                res.status(500).json({ error: String(err) });
            }
        });
        // Add /api/reset endpoint that redirects to /api/reset-cache
        // This is for compatibility with existing calls
        this.app.get('/api/reset', (_req, res) => {
            if (!this.poller) {
                res.status(503).json({ error: 'Server not initialized' });
                return;
            }
            this.poller.resetCache();
            res.json({ message: 'Cache reset successfully via /api/reset' });
            console.log('🔄 Cache reset via /api/reset endpoint');
        });
        // Health check endpoint
        this.app.get('/health', (_req, res) => {
            res.json({
                status: 'healthy',
                server: 'typescript',
                version: '1.0.0'
            });
        });
        // Catch-all handler: send back React's index.html file for client-side routing
        this.app.get('*', (_req, res) => {
            const buildPath = path_1.default.join(__dirname, '../../dist/index.html');
            try {
                res.sendFile(buildPath);
            }
            catch {
                res.status(404).json({ error: 'Not found' });
            }
        });
    }
}
exports.HttpServer = HttpServer;
//# sourceMappingURL=httpServer.js.map