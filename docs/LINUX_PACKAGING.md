# Linux Packaging Guide

## Overview

This guide explains how the `.deb` package is properly configured for Linux distribution.

## What Was Fixed

Based on your friend's troubleshooting, we've addressed all the packaging issues:

### 1. Project Structure

The application now has the proper directory structure for Compose Desktop Linux packaging:

```
super_metroid_auto_tracker/
 └── src/
     └── main/
         ├── compose/
         │   └── resources/
         │       ├── Super_Metroid_Auto_Tracker.png  # Icon file
         │       └── supermetroidautotracker.desktop # Desktop entry
         ├── kotlin/
         ├── proto/
         └── resources/
```

### 2. Desktop Entry File

**Location:** `src/main/compose/resources/supermetroidautotracker.desktop`

```ini
[Desktop Entry]
Name=Super Metroid Auto Tracker
Comment=An auto-tracker for Super Metroid randomizer
Exec="/opt/supermetroidautotracker/bin/Super Metroid Auto Tracker"
Icon=/opt/supermetroidautotracker/lib/Super_Metroid_Auto_Tracker.png
Type=Application
Categories=Game;ActionGame;
Terminal=false
```

**Key points:**
- `Exec` path points to the installed binary location
- `Icon` path points to where the icon will be installed
- `Categories=Game;ActionGame;` ensures it appears under "Games" in the launcher
- `Terminal=false` prevents a terminal window from opening

### 3. Gradle Configuration

**File:** `build.gradle.kts`

The Linux-specific configuration in the `nativeDistributions` block:

```kotlin
linux {
    targetFormats(TargetFormat.Deb)
    packageName = "supermetroidautotracker"
    packageVersion = "1.0.0"
    description = "Super Metroid Auto Tracker"
    shortcut = true
    iconFile.set(project.file("src/main/compose/resources/Super_Metroid_Auto_Tracker.png"))
    menuGroup = "Games"
}
```

**What each property does:**
- `targetFormats(TargetFormat.Deb)` - Builds `.deb` package for Debian/Ubuntu
- `packageName` - The package identifier (no spaces, lowercase)
- `packageVersion` - Version number for the package
- `shortcut = true` - Creates desktop menu entry automatically
- `iconFile` - Points to the icon in the compose/resources directory
- `menuGroup = "Games"` - Places the app in the Games category

### 4. Icon File

The icon has been copied to the proper location:
- **Source:** `src/main/resources/icon.png`
- **Destination:** `src/main/compose/resources/Super_Metroid_Auto_Tracker.png`

This ensures the icon is bundled with the `.deb` package and installed correctly.

## Building the Package

### On Linux (Native Build)

```bash
./gradlew clean packageDeb
```

The `.deb` file will be created at:
```
build/compose/binaries/main/deb/supermetroidautotracker_2.0.0_amd64.deb
```

### Using GitHub Actions (Cross-Platform)

The repository is configured with GitHub Actions to build packages for all platforms:

1. Go to the "Actions" tab on GitHub
2. Select the "Build" or "Release" workflow
3. Click "Run workflow"
4. Download the built `.deb` from the artifacts

## Installing the Package

### Standard Installation

```bash
sudo dpkg -i supermetroidautotracker_2.0.0_amd64.deb
```

### If Dependencies Are Missing

```bash
sudo apt-get install -f
```

This will automatically install any missing dependencies.

## Verification

After installation, verify everything works:

### 1. Check Files Are Installed

```bash
# Check binary
ls -l /opt/supermetroidautotracker/bin/

# Check icon
ls -l /opt/supermetroidautotracker/lib/Super_Metroid_Auto_Tracker.png

# Check desktop entry
ls -l /usr/share/applications/supermetroidautotracker*.desktop
```

### 2. Check Launcher

- Open your application menu
- Navigate to "Games"
- Look for "Super Metroid Auto Tracker"
- Verify the icon appears correctly

### 3. Run the Application

Click the launcher or run from terminal:

```bash
/opt/supermetroidautotracker/bin/Super\ Metroid\ Auto\ Tracker
```

## Troubleshooting

### Launcher Doesn't Appear

1. Update the desktop database:
   ```bash
   update-desktop-database ~/.local/share/applications/
   ```

2. Reload your desktop environment (logout/login)

### Icon Missing

Check if the icon exists:
```bash
ls -l /opt/supermetroidautotracker/lib/*.png
```

If missing, the build may not have included it. Rebuild with `./gradlew clean packageDeb`.

### Wrong Category

If the app appears under "Unknown" instead of "Games":

1. Check the desktop file:
   ```bash
   cat /usr/share/applications/supermetroidautotracker*.desktop
   ```

2. Ensure `Categories=Game;ActionGame;` is present

3. Update desktop database:
   ```bash
   update-desktop-database ~/.local/share/applications/
   ```

## Uninstalling

```bash
sudo dpkg -r supermetroidautotracker
```

Or to remove configuration files as well:

```bash
sudo dpkg --purge supermetroidautotracker
```

## Notes

- The package name is `supermetroidautotracker` (no spaces)
- The application installs to `/opt/supermetroidautotracker/`
- The desktop entry installs to `/usr/share/applications/`
- User data is stored in `~/.smtracker/`
- The `.deb` package includes the JRE, so Java doesn't need to be installed separately

## For Developers

### Testing Package Installation

```bash
# Build the package
./gradlew clean packageDeb

# Install locally
sudo dpkg -i build/compose/binaries/main/deb/supermetroidautotracker_*.deb

# Test the application
/opt/supermetroidautotracker/bin/Super\ Metroid\ Auto\ Tracker

# Uninstall
sudo dpkg -r supermetroidautotracker
```

### Docker Testing

You can test the package in a clean Ubuntu environment using Docker:

```bash
# Build the Docker image with the .deb
docker build -t smtracker-test .

# Run and test
docker run -it smtracker-test bash
```

(See Dockerfile in the repository)

