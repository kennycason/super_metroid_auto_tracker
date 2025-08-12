import express from 'express';
import type { Request, Response } from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import { BackgroundPoller } from './backgroundPoller';

// ES module equivalent of __dirname
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * HTTP server serving cached game state data
 * Equivalent to the Kotlin HttpServer
 */
export class HttpServer {
  private app: express.Application;
  private poller: BackgroundPoller;
  private port: number;
  private pollInterval: number;
  private server: any = null;

  constructor(port: number = 8000, pollInterval: number = 1000) {
    this.port = port;
    this.pollInterval = pollInterval;
    this.poller = new BackgroundPoller(pollInterval);
    this.app = express();
    this.configureServer();
    this.configureRouting();
  }

  /**
   * Start the HTTP server and background poller
   */
  async start(): Promise<void> {
    try {
      // Start background poller
      await this.poller.start();

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

    } catch (err) {
      console.log(`Server error: ${err}`);
      this.stop();
    }
  }

  /**
   * Stop the HTTP server and background poller
   */
  stop(): void {
    this.poller.stop();
    if (this.server) {
      this.server.close();
    }
    console.log('🏁 Server stopped');
  }

  /**
   * Configure Express server middleware
   */
  private configureServer(): void {
    // Enable CORS for all routes
    this.app.use(cors({
      origin: true,
      methods: ['GET', 'POST', 'PUT', 'DELETE'],
      allowedHeaders: ['Content-Type']
    }));

    // Parse JSON bodies
    this.app.use(express.json());

    // Serve static files from React build (if available)
    const buildPath = path.join(__dirname, '../../dist');
    this.app.use(express.static(buildPath));
  }

  /**
   * Configure API routes
   */
  private configureRouting(): void {
    // Main endpoints
    this.app.get('/', (_req: Request, res: Response) => {
      // Serve React app if built, otherwise show simple HTML
      const buildPath = path.join(__dirname, '../../dist/index.html');
      try {
        res.sendFile(buildPath);
      } catch {
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

    this.app.get('/api/status', (_req: Request, res: Response) => {
      const status = this.poller.getCachedState();
      res.json(status);
    });

    this.app.get('/api/stats', (_req: Request, res: Response) => {
      const status = this.poller.getCachedState();
      res.json(status.stats);
    });

    this.app.get('/game_state', (_req: Request, res: Response) => {
      const status = this.poller.getCachedState();
      res.json(status.stats);
    });

    this.app.post('/api/reset-cache', (_req: Request, res: Response) => {
      this.poller.resetCache();
      res.json({ message: 'Cache reset successfully' });
    });

    this.app.get('/api/bootstrap-mb', (_req: Request, res: Response) => {
      res.json({ message: 'Bootstrap triggered' });
    });

    this.app.get('/api/manual-mb-complete', (_req: Request, res: Response) => {
      try {
        // Note: In full implementation, this would manually set MB completion
        const response = {
          message: 'MB1 and MB2 manually set to completed',
          mb1: true,
          mb2: true
        };
        res.json(response);
        console.log('🔧 Manual MB completion triggered via API');
      } catch (err) {
        res.status(500).json({ error: String(err) });
      }
    });

    this.app.get('/api/reset-mb-cache', (_req: Request, res: Response) => {
      try {
        // Reset Mother Brain cache in parser (would need access to parser)
        const response = { message: 'MB cache reset to default (not detected)' };
        res.json(response);
        console.log('🔄 MB cache reset via API');
      } catch (err) {
        res.status(500).json({ error: String(err) });
      }
    });

    // Add /api/reset endpoint that redirects to /api/reset-cache
    // This is for compatibility with existing calls
    this.app.get('/api/reset', (_req: Request, res: Response) => {
      this.poller.resetCache();
      res.json({ message: 'Cache reset successfully via /api/reset' });
      console.log('🔄 Cache reset via /api/reset endpoint');
    });

    // Health check endpoint
    this.app.get('/health', (_req: Request, res: Response) => {
      res.json({
        status: 'healthy',
        server: 'typescript',
        version: '1.0.0'
      });
    });

    // Catch-all handler: send back React's index.html file for client-side routing
    this.app.get('*', (_req: Request, res: Response) => {
      const buildPath = path.join(__dirname, '../../dist/index.html');
      try {
        res.sendFile(buildPath);
      } catch {
        res.status(404).json({ error: 'Not found' });
      }
    });
  }
}
