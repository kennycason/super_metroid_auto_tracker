plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "com.supermetroid"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

application {
    mainClass.set("MainKt")
}

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
    }
}

tasks.register<Copy>("copyGrapplingBeamScript") {
    from("explore/GrapplingBeamEnabler.kt")
    into("src/main/kotlin")
    rename { "Main.kt" }
}

tasks.named("compileKotlin") {
    dependsOn("copyGrapplingBeamScript")
}
