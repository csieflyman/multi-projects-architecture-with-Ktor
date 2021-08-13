plugins {
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.5.21")
    implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
}