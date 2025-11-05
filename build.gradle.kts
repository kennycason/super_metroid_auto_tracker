import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
    id("com.google.protobuf") version "0.9.4"
}

group = "com.supermetroid"
version = "1.0.0"

repositories {
    mavenCentral()
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
    
    // MP3 support
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")
    
    // gRPC for SNI integration
    implementation("io.grpc:grpc-kotlin-stub:1.4.0")
    implementation("io.grpc:grpc-protobuf:1.58.0")
    implementation("io.grpc:grpc-netty:1.58.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    
    // Protobuf compiler
    implementation("com.google.protobuf:protobuf-java:3.25.1")
    
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

// Fat JAR task - creates a single executable JAR with all dependencies
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("SuperMetroidAutoTracker")
    archiveVersion.set("")
    
    manifest {
        attributes["Main-Class"] = "com.supermetroid.ui.SuperMetroidTrackerKt"
    }
    
    // Include all dependencies in the JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
    
    // Exclude signature files to avoid conflicts
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Removed application plugin - using Compose Desktop instead

compose.desktop {
    application {
        mainClass = "com.supermetroid.ui.SuperMetroidTrackerKt"
        
        // Add JVM arguments to include required Java modules for Logback
        jvmArgs += listOf(
            "--add-modules", "java.naming",
            "--add-exports", "java.naming/com.sun.jndi.ldap=ALL-UNNAMED"
        )
        
        nativeDistributions {
            // Bundle all modules including JRE for true cross-platform distribution
            includeAllModules = true
            
            packageName = "Super Metroid Auto Tracker"
            packageVersion = "1.0.0"
            description = "Super Metroid Auto Tracker - Kotlin Compose Desktop"
            copyright = "© 2025 Super Metroid Tracker"
            vendor = "Super Metroid Community"
            
            // Include Java naming module in the packaged runtime
            modules("java.naming", "java.sql")
            
            // Global target formats for cross-platform builds
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            
            macOS {
                bundleID = "com.supermetroid.autotracker"
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
            
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            
            linux {
                packageName = "supermetroidautotracker"
                packageVersion = "1.0.0"
                description = "Super Metroid Auto Tracker"
                shortcut = true
                iconFile.set(project.file("src/main/resources/icon.png"))
                menuGroup = "Games"
            }
        }
    }
}

// Clean JVM-only structure

// Protobuf configuration
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.58.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}
