#!/bin/bash

# Build and run the simple_udp_test.kt script

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
            executable("udp_test") {
                entryPoint = "MainKt.main"
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            kotlin.srcDir("explore")
        }
    }
}

tasks.register<Copy>("copyTestScript") {
    from("explore/simple_udp_test.kt")
    into("src/nativeMain/kotlin")
    rename { "Main.kt" }
}

tasks.named("compileKotlinNative") {
    dependsOn("copyTestScript")
}
EOF

# Backup the original build file
cp build.gradle.kts build.gradle.kts.backup

# Replace the build file with our temporary one
mv build.gradle.kts.temp build.gradle.kts

# Create the source directory
mkdir -p src/nativeMain/kotlin

# Build the test script
echo "Building test script..."
./gradlew linkDebugExecutableNative_udp_test

# Run the test script
echo "Running test script..."
./build/bin/native/debugExecutable/udp_test.kexe

# Restore the original build file
mv build.gradle.kts.backup build.gradle.kts

# Clean up
rm -rf src/nativeMain/kotlin/Main.kt

echo "Test completed."
