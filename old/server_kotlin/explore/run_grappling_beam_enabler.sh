#!/bin/bash

# Build and run the GrapplingBeamEnabler.kt script

# Navigate to the project root
cd "$(dirname "$0")/.."

# Create a temporary build file
cat > build.gradle.kts.temp << EOF
plugins {
    kotlin("multiplatform") version "1.9.21"
}

group = "com.supermetroid"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable("grappling_beam") {
                entryPoint = "MainKt.main"
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            // Only include the explore directory with our standalone implementation
            kotlin.srcDir("explore")
            // No other dependencies needed since we've made it completely standalone
        }
    }
}

tasks.register<Copy>("copyGrapplingBeamScript") {
    from("explore/GrapplingBeamEnabler.kt")
    into("src/nativeMain/kotlin")
    rename { "Main.kt" }
}

tasks.named("compileKotlinNative") {
    dependsOn("copyGrapplingBeamScript")
}
EOF

# Backup the original build file
cp build.gradle.kts build.gradle.kts.backup

# Replace the build file with our temporary one
mv build.gradle.kts.temp build.gradle.kts

# Create the source directory
mkdir -p src/nativeMain/kotlin

# Build the script
echo "Building Grappling Beam Enabler..."
./gradlew linkDebugExecutableNative

# Run the script
echo "Running Grappling Beam Enabler..."
echo "Make sure RetroArch is running with Super Metroid loaded!"
echo "-----------------------------------------------------"
# The executable path is based on the Gradle configuration
./build/bin/native/debugExecutable/server_kotlin.kexe

# Restore the original build file
mv build.gradle.kts.backup build.gradle.kts

# Clean up
rm -rf src/nativeMain/kotlin/Main.kt

echo ""
echo "NOTE: If you want a more reliable way to run this program,"
echo "use the ./explore/compile_and_run_grappling_beam.sh script instead."
echo "It uses a minimal Gradle configuration that avoids conflicts with the main project."

echo "Grappling Beam Enabler completed."
