import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "com.supermetroid"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    // LibGDX snapshot repository (needed for 2.2.4-SNAPSHOT controllers)
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
}

dependencies {
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3") // For Swing Main dispatcher
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    
    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.1")
    
    // LibGDX Controllers for gamepad support
    implementation("com.badlogicgames.gdx:gdx:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.12.1")
    // LibGDX Controllers - EXACT same versions as working ninjaturdle project
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-core:2.2.4-SNAPSHOT")
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-desktop:2.2.4-SNAPSHOT")
    
    // Jamepad - the key missing dependency for controller support!
    implementation("com.badlogicgames.jamepad:jamepad:2.30.0.0")
    
    // LibGDX core and platform natives (NOT headless - we need full LibGDX)
    implementation("com.badlogicgames.gdx:gdx:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-desktop")
    
    // Native controller support via LibGDX
    
    // Native gamepad support
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.strikt:strikt-core:0.34.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Removed application plugin - using Compose Desktop instead

compose.desktop {
    application {
        mainClass = "com.supermetroid.ui.SuperMetroidTrackerKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Super Metroid Auto Tracker"
            packageVersion = "2.0.0"
            description = "Super Metroid Auto Tracker - Kotlin Compose Desktop"
            copyright = "© 2025 Super Metroid Tracker"
            vendor = "Super Metroid Community"
            
            macOS {
                bundleID = "com.supermetroid.autotracker"
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
        }
    }
}

// Clean JVM-only structure

// Remove old server copy task - we're building a Compose app now
