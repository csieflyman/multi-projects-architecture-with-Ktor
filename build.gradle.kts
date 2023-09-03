plugins {
    id("org.jetbrains.kotlinx.kover")
}

repositories {
    mavenCentral()
}

dependencies {
    kover(project(":infra"))
    kover(project(":projects:ops"))
    kover(project(":projects:club"))
}