import { HttpServer } from './httpServer';
import { parseBackendConfig } from './emulatorBackend';

/**
 * Main entry point for Super Metroid Tracker TypeScript server
 * Equivalent to the Kotlin Main.kt
 */
async function main(): Promise<void> {
  const args = process.argv.slice(2);

  // Parse named arguments first
  const backendConfig = parseBackendConfig(args);

  // Parse positional arguments (skip named arguments)
  const positionalArgs = args.filter(arg => !arg.startsWith('--'));
  const port = positionalArgs[0] ? parseInt(positionalArgs[0]) : 8080;
  const pollInterval = positionalArgs[1] ? parseInt(positionalArgs[1]) : 1000;

  console.log('🎮 Starting Super Metroid Tracker (TypeScript)');
  console.log(`Port: ${port}, Poll Interval: ${pollInterval}ms`);
  console.log(`Backend: ${backendConfig.type}`);

  const server = new HttpServer(port, pollInterval, backendConfig);

  // Handle shutdown signals
  process.on('SIGINT', () => {
    console.log('\n🛑 Shutdown requested');
    server.stop();
    process.exit(0);
  });

  process.on('SIGTERM', () => {
    console.log('\n🛑 Shutdown requested');
    server.stop();
    process.exit(0);
  });

  try {
    await server.start();
  } catch (err) {
    console.log(`❌ Server failed to start: ${err}`);
    server.stop();
    process.exit(1);
  }
}

// Run the server when this module is executed
main().catch(err => {
  console.error('❌ Fatal error:', err);
  process.exit(1);
});

export { main };
