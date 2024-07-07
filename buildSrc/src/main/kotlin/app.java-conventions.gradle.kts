import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("org.jetbrains.kotlinx.kover")
}

group = "com.fanpoll"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
        javaParameters = true
        suppressWarnings = true
        freeCompilerArgs.add("-opt-in=kotlin.ExperimentalStdlibApi")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
}

tasks.shadowJar {
    mergeServiceFiles()
}

kover {
    reports {
        filters {
            includes {
                classes("fanpoll.*")
            }
        }
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
        }
    }
}

