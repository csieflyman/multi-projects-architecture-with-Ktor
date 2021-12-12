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

    // https://kotlinlang.org/docs/whatsnew1530.html#improvements-to-type-inference-for-recursive-generic-types
    // COMPATIBILITY => update intellij kotlin plugin to early access preview 1.6.x

    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.languageVersion = "1.6"
        kotlinOptions.javaParameters = true
        kotlinOptions.suppressWarnings = true
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalStdlibApi"
        //kotlinOptions.useIR = true (1.4 in Alpha)
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.languageVersion = "1.6"
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