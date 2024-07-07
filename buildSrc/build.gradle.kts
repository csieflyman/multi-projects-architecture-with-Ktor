plugins {
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.0")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("com.bmuschko:gradle-docker-plugin:9.4.0")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.8.2")
}