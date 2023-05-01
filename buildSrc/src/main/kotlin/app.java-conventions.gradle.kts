plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
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

// =============================== Compile ===============================

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.languageVersion = "1.8"
        kotlinOptions.javaParameters = true
        kotlinOptions.suppressWarnings = true
        compilerOptions.freeCompilerArgs.add("-opt-in=kotlin.ExperimentalStdlibApi")
        //kotlinOptions.useIR = true (1.4 in Alpha)
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.languageVersion = "1.8"
        kotlinOptions.javaParameters = true
        kotlinOptions.suppressWarnings = true
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
}

tasks.shadowJar {
    mergeServiceFiles()
}