const { spawn } = require('child_process');

console.log('🧪 Testing Timer Fix...');
console.log('This script will start the dev server and you can manually test the timer.');
console.log('');
console.log('Test Steps:');
console.log('1. Open http://localhost:3000 in your browser');
console.log('2. Click the START button on the timer');
console.log('3. Verify that the timer display updates every 100ms');
console.log('4. Test PAUSE and RESET buttons');
console.log('5. Test the H+/-, M+/-, S+/- adjustment buttons');
console.log('');
console.log('Expected behavior:');
console.log('- Timer should visually update when running');
console.log('- No more "timer not updating" issue');
console.log('- All buttons should work correctly');
console.log('');

// Start the development server
const devServer = spawn('npm', ['run', 'dev'], {
  stdio: 'inherit',
  shell: true
});

devServer.on('close', (code) => {
  console.log(`Dev server exited with code ${code}`);
});

// Handle Ctrl+C
process.on('SIGINT', () => {
  console.log('\n🛑 Stopping test...');
  devServer.kill('SIGINT');
  process.exit(0);
});