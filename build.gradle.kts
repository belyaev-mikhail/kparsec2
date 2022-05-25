import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.platform.CommonPlatforms

plugins {
    kotlin("multiplatform") version "1.6.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.2"
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
        nodejs { }
        browser { }
    }
    linuxX64 {}

    targets.all {
        if (this != metadata {}) compilations.create("benchmarks")
    }

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
        val commonBenchmarks by creating {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.2")
                dependsOn(commonMain.get())
            }
        }
        all {
            if (name.endsWith("Benchmarks") && this != commonBenchmarks) {
                dependsOn(commonBenchmarks)
            }
        }
    }
}

benchmark {
    configurations {
        val main by getting {
            mode = "avgt"
            reportFormat = "scsv"
            outputTimeUnit = "ns"
        }
    }

    targets {
        register("jvmBenchmarks")
        register("jsBenchmarks")
        register("linuxX64Benchmarks")
//        register("js")
//        register("native")
    }
}