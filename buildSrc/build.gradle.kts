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

    constraints {
        implementation("org.apache.logging.log4j:log4j-core:2.16.0") {
            because("Log4jShell security vulnerability")
        }
    }
}