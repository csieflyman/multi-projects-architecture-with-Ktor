plugins {
    id("fanpoll.java-conventions")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("plugin.serialization") version "1.5.21"
}

dependencies {
    implementation(project(":infra"))
}

configurations.api.get().isCanBeResolved = true

tasks.shadowJar {
    configurations = listOf(project.configurations.api.get().setTransitive(false))
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib:.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:.*"))
    }
    mergeServiceFiles()
}