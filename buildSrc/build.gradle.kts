plugins {
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.0")
    implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
    implementation("com.bmuschko:gradle-docker-plugin:7.1.0")

    // https://github.com/kotest/kotest-gradle-plugin
    // get NPE when run gradle task 'kotest'. It may be incompatible with kotest 5.0.2
    //implementation("io.kotest:kotest-gradle-plugin:0.3.9")

    constraints {
        implementation("org.apache.logging.log4j:log4j-core:2.17.0") {
            because(
                "upgrade log4j-core from 2.14.1 to 2.16.0 required by shadow jar gradle plugin " +
                        "due to Log4jShell security vulnerability. Although it would not affect server at runtime"
            )
        }
    }
}

// =============================== Compile ===============================

tasks {
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