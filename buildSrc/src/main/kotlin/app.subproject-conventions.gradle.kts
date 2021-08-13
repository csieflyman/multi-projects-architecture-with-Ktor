plugins {
    id("app.java-conventions")
    id("com.github.johnrengelman.shadow")
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