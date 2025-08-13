import { HttpServer } from './httpServer';

/**
 * Main entry point for Super Metroid Tracker TypeScript server
 * Equivalent to the Kotlin Main.kt
 */
async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const port = args[0] ? parseInt(args[0]) : 8080;
  const pollInterval = args[1] ? parseInt(args[1]) : 1000;

  console.log('🎮 Starting Super Metroid Tracker (TypeScript)');
  console.log(`Port: ${port}, Poll Interval: ${pollInterval}ms`);

  const server = new HttpServer(port, pollInterval);

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
