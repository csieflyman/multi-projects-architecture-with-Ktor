import gradle.kotlin.dsl.accessors._658d1437a6ce43285bbd8d098b7c9d8c.runtimeClasspath

plugins {
    id("app.java-conventions")
    id("app.test-conventions")
}

dependencies {
    implementation(project(":infra"))
}

tasks.shadowJar {
    // https://github.com/johnrengelman/shadow/issues/448
    configurations {
        shadow {
            runtimeClasspath.get().isCanBeResolved = true
            api.get().setTransitive(false)
        }
    }
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib:.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7:.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:.*"))
    }
}