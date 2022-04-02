import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    kotlin("multiplatform") version "1.5.30"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.2"
}

group = "ru.spbstu"
version = "0.0.0.1"

repositories {
    mavenCentral()
    maven("https://maven.vorpal-research.science")
}

kotlin {
    jvm {
        compilations.create("benchmarks")
    }
    js(LEGACY) {
        nodejs {
            compilations.create("benchmarks")
        }
        browser {}
    }
    linuxX64 {
        compilations.create("benchmarks")
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
            reportFormat = "csv"
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