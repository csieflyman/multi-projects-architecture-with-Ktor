// https://github.com/Kotlin/kotlinx-kover#apply-plugin-to-multi-module-project
plugins {
    id("org.jetbrains.kotlinx.kover") version "0.4.4"
}

repositories {
    mavenCentral()
}

tasks.koverHtmlReport {
    isEnabled = true
}

tasks.koverXmlReport {
    isEnabled = true
}

kover {
    isEnabled = true                        // false to disable instrumentation of all test tasks in all modules
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.INTELLIJ)
    intellijEngineVersion.set("1.0.639")
    jacocoEngineVersion.set("0.8.7")
    generateReportOnCheck.set(true)         // false to do not execute `koverReport` task before `check` task
}