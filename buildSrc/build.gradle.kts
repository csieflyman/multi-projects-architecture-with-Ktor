plugins {
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.10")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("com.bmuschko:gradle-docker-plugin:9.3.2")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.7.3")

    // https://kotlinlang.org/docs/whatsnew18.html#resolution-of-kotlin-gradle-plugins-transitive-dependencies
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-sam-with-receiver:1.9.10")
    }
}