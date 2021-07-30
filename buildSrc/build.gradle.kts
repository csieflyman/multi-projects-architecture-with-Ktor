plugins {
    kotlin("jvm") version "1.5.21"
    id("org.gradle.kotlin.kotlin-dsl") version "2.1.6"
    id("org.gradle.kotlin.embedded-kotlin") version "2.1.6"
    //`kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
}