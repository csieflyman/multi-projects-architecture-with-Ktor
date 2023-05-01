plugins {
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.8.21")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("com.bmuschko:gradle-docker-plugin:9.2.1")
    implementation("org.jetbrains.kotlinx.kover:org.jetbrains.kotlinx.kover.gradle.plugin:0.6.1")

    // https://kotlinlang.org/docs/whatsnew18.html#resolution-of-kotlin-gradle-plugins-transitive-dependencies
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-sam-with-receiver:1.8.21")
    }
}

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