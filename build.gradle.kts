import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    kotlin("multiplatform") version "1.5.30"
}

group = "ru.spbstu"
version = "0.0.0.1"

repositories {
    mavenCentral()
    maven("https://maven.vorpal-research.science")
}

kotlin {
    jvm {}
    js(LEGACY) {
        nodejs {}
        browser {}
    }
    linuxX64 {}
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("ru.spbstu:kotlinx-warnings:${getKotlinPluginVersion()}")
                implementation("ru.spbstu:kotlin-wheels:0.0.1.3")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}