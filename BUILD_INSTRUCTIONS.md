# Build Instructions

## For Users

### Download Pre-built Executables

1. Go to the [Releases](https://github.com/kennycason/super_metroid_auto_tracker/releases) page
2. Download the appropriate file for your platform:
   - **macOS**: `SuperMetroidAutoTracker-macOS.dmg`
   - **Windows**: `SuperMetroidAutoTracker-Windows.msi`
   - **Linux**: `SuperMetroidAutoTracker-Linux.deb`

### Installation

- **macOS**: Double-click the `.dmg` file and drag to Applications
- **Windows**: Double-click the `.msi` file and follow the installer
- **Linux**: `sudo dpkg -i SuperMetroidAutoTracker-Linux.deb`

## For Developers

### Manual Builds (GitHub Actions)

1. Go to the [Actions](https://github.com/kennycason/super_metroid_auto_tracker/actions) tab
2. Click "Build Cross-Platform" workflow
3. Click "Run workflow" button
4. Wait for builds to complete (5-10 minutes)
5. Download artifacts from the completed run

### Local Development

```bash
# Run the app locally
./gradlew run

# Build for current platform only
./gradlew packageDistributionForCurrentOS

# Build fat JAR (for testing, not distribution)
./gradlew fatJar
```

### Creating Releases

1. Create a new release on GitHub with a version tag (e.g., `v2.1.0`)
2. The release workflow will automatically build and attach platform-specific installers
3. Users can download directly from the release page

## Technical Details

- **Build System**: Gradle + Compose Desktop
- **Java Version**: 17
- **Cross-Platform**: Uses GitHub Actions to build on macOS, Windows, and Linux
- **Native Libraries**: Skiko libraries are included in platform-specific builds
- **Self-Contained**: All builds include bundled JRE (no Java installation required)

## Troubleshooting

### "Cannot find libskiko-linux-x64.so.sha256"
- Use the pre-built `.deb` package instead of running from source
- The native libraries are included in the packaged builds

### Build fails locally
- Ensure you have Java 17+ installed
- Run `./gradlew clean` and try again
- For cross-platform builds, use GitHub Actions instead

### GitHub Actions build fails
- Check the Actions tab for error details
- Ensure all dependencies are properly configured
- Contact the maintainer if issues persist
