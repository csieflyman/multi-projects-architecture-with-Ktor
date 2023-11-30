plugins {
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.21")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("com.bmuschko:gradle-docker-plugin:9.4.0")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.7.5")
}