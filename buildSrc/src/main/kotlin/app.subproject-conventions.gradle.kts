plugins {
    id("app.java-conventions")
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
}

// https://github.com/gradle/gradle/issues/14889
tasks.register("prepareKotlinBuildScriptModel") {}