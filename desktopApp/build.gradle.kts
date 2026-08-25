import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
    sourceSets {
        main {
            kotlin.srcDirs(
                "src/main/kotlin",
                "../app/src/main/java"
            )
            kotlin.exclude("**/MainActivity.kt")
            kotlin.exclude("**/AndroidAssetProvider.kt")
            resources.srcDirs(
                "src/main/resources"
            )
        }
    }
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    
    // KotlinX Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    // Ktor for Local Wi-Fi Network & WebSockets
    val ktorVersion = "2.3.11"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
}

compose.desktop {
    application {
        mainClass = "com.gamechest.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "TheGameChest"
            packageVersion = "1.0.0"
            description = "The Game Chest - Digital Board Game Hub"
            vendor = "The Game Chest"
            copyright = "© 2026 The Game Chest"
            
            windows {
                menuGroup = "The Game Chest"
                upgradeUuid = "a3f5c71b-6893-49d7-84e1-7e89ab31c590"
            }
        }
    }
}
