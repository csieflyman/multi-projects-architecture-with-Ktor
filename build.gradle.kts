import com.github.gradle.node.npm.proxy.ProxySettings
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.unbrokendome.gradle.plugins.gitversion.core.RuleContext
import org.unbrokendome.gradle.plugins.gitversion.version.SemVersion
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "1.5.21"
    kotlin("kapt") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.flywaydb.flyway") version "7.11.2"
    id("org.unbroken-dome.gitversion") version "0.10.0"
    //id("io.gitlab.arturbosch.detekt") version "1.15.0-RC1"
    // https://github.com/node-gradle/gradle-node-plugin
    // https://plugins.gradle.org/plugin/com.github.node-gradle.node
    id("com.github.node-gradle.node") version "3.0.1"
}

group = "com.ktor-example"

application {
    mainClassName = "fanpoll.ApplicationKt"
    //mainClass.set("fanpoll.ApplicationKt")// shadow plugin 6.1.0 is unsupported
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Duser.timezone=UTC", "-Dkotlinx.coroutines.debug")
    //-Dconfig.file=$local_path/application-local.conf
    //-Dlogback.configurationFile=$local_path/logback-local.xml
    //-Dswagger-ui.dir=$local_path/swagger-ui
}

repositories {
    mavenCentral()
}

val kotlinVersion = "1.5.21" // the same with plugin version above
val flywayVersion = "7.11.2" // the same with plugin version above
val ktorVersion = "1.6.1"
val koinVersion = "3.1.2"
val exposedVersion = "0.32.1"
val jacksonVersion = "2.12.4"

dependencies {
    // =============== kotlin, kotlinx ===============
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    // ===============  ktor ===============
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")

    // ===============  koin (DI) ===============
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    //implementation("io.insert-koin:koin-core-ext:$koinVersion")
    //implementation("io.insert-koin:koin-test-junit5:$koinVersion")

    // =============== database ===============
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    runtimeOnly("org.postgresql:postgresql:42.2.23")

    // ===============  AWS Java SDK Version 2 ===============
    implementation(platform("software.amazon.awssdk:bom:2.16.34"))

    implementation("software.amazon.awssdk:netty-nio-client")

    implementation("software.amazon.awssdk:firehose")

    implementation("software.amazon.awssdk:ses")
    implementation("javax.mail:javax.mail-api:1.6.2")
    implementation("com.sun.mail:javax.mail:1.6.2")

    // ===============  third-party service ===============
    implementation("com.google.firebase:firebase-admin:7.1.1")

    // =============== utils - feature ===============
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    implementation("org.freemarker:freemarker:2.3.31")
    implementation("io.konform:konform-jvm:0.3.0")
    implementation("at.favre.lib:bcrypt:0.9.0")

    // =============== utils - config file parser ===============
    implementation("com.ufoscout.properlty:properlty-kotlin:1.9.0")
    implementation("io.github.config4k:config4k:0.4.2")
    //implementation("com.charleskorn.kaml:kaml:0.19.0")

    implementation("org.apache.poi:poi:5.0.0")
    implementation("org.apache.poi:poi-ooxml:5.0.0")
    implementation("org.apache.commons:commons-csv:1.8")

    // =============== utils - bean ===============
    implementation("com.github.rocketraman:kpropmap:0.0.2")

    //implementation("org.mapstruct:mapstruct:1.4.2.Final")
    //kapt("org.mapstruct:mapstruct-processor:1.4.2.Final")

    // Kotlin DataClass DeepCopy
    //implementation("com.bennyhuo.kotlin:deepcopy-reflect:1.1.0")
    //kapt ("com.bennyhuo.kotlin:deepcopy-compiler:1.3.72")
    //implementation("com.bennyhuo.kotlin:deepcopy-runtime:1.3.72")

    // =============== utils - logging ===============
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.10")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    // =============== utils - general ===============
    implementation("com.github.kittinunf.result:result:5.1.0")
    implementation("com.github.kittinunf.result:result-coroutines:4.0.0")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("com.google.guava:guava:30.1.1-jre")

    // jackson only used for openapi
    // maybe need in the future https://github.com/FasterXML/jackson-modules-java8
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-blackbird:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // =============== test ===============
    //testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

// =============================== Compile ===============================

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.languageVersion = "1.5"
        kotlinOptions.javaParameters = true
        kotlinOptions.suppressWarnings = true
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalStdlibApi"
        //kotlinOptions.useIR = true (1.4 in Alpha)
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.languageVersion = "1.5"
        kotlinOptions.javaParameters = true
        kotlinOptions.suppressWarnings = true
    }
}

tasks.withType<JavaCompile> {
    options.isFork = true
}

// =============================== Flyway Plugin ===============================

flyway {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/ktor-example"
    user = "ktor"
    password = "ktor"
    defaultSchema = "ktor-example"
}

// =============================== Git Plugin ===============================
// https://github.com/unbroken-dome/gradle-gitversion-plugin

tasks.determineGitVersion {
    outputs.upToDateWhen { false }
}

// my git branch naming convention
val devBranchName = "dev"
val releaseBranchName = "main"
val branchToEnvMap: Map<String, String> = mapOf(devBranchName to "dev", releaseBranchName to "prod")

gitVersion {
    rules {
        val branches = branchToEnvMap.keys.toList()
        branches.forEach { branch ->
            onBranch(branch) {
                setVersionByTag(this, branch)
            }
        }
    }
}

fun setVersionByTag(ruleContext: RuleContext, branch: String) {
    val tagPrefix = if (branch == releaseBranchName) "" else "$branch-" // my git tag naming convention
    val tag = ruleContext.findLatestTag("""${tagPrefix}(\d+)\.(\d+)\.(\d+)""".toPattern())
    if (tag != null) {
        with(ruleContext.version) {
            major = tag.matches.getAt(1).toInt()
            minor = tag.matches.getAt(2).toInt()
            patch = tag.matches.getAt(3).toInt()
            setPrereleaseTag(branch)
            setBuildMetadata("${ruleContext.countCommitsSince(tag)}-${tag.commit.id}")
        }
    } else {
        ruleContext.version.set(1, 0, 0, branch, ruleContext.repository.head!!.id)
    }
}

project.version = gitVersion.determineVersion()

// =============================== Distribution & Shadow Plugin ===============================

val shadowEnvDistZip by tasks.registering {

    group = "distribution"

    finalizedBy(tasks.shadowDistZip)

    doLast {
        delete("src/dist")

        val semVersion = (project.version as SemVersion)
        val tagVersion = "${semVersion.major}.${semVersion.minor}.${semVersion.patch}"
        val branch = semVersion.prereleaseTag!!
        val env = branchToEnvMap[branch]
        println("========================================")
        println("semVersion = $semVersion")
        println("env = $env")
        println("========================================")

        copy {
            from("resources/application.conf", "resources/logback.xml")
            into("src/dist")
            filter<ReplaceTokens>(
                "tokens" to mapOf(
                    "gitTagVersion" to tagVersion,
                    "gitCommitVersion" to semVersion.toString(),
                    "env" to env,
                    "buildTime" to DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
                )
            )
        }
        copy {
            from("deploy/config/$env")
            into("src/dist")
        }
        copy {
            from("deploy/scripts")
            into("src/dist/scripts")
        }
    }
}

// https://imperceptiblethoughts.com/shadow

tasks.shadowDistZip {

    outputs.upToDateWhen { false }

    if (project.hasProperty("local.archive.destinationDirectory")) {
        destinationDirectory.set(
            file(
                Paths.get(project.property("local.archive.destinationDirectory")!!.toString()).toUri().toURL()
            )
        )
    }

    val semVersion = (project.version as SemVersion)
    val tagVersion = "${semVersion.major}.${semVersion.minor}.${semVersion.patch}"
    val branch = semVersion.prereleaseTag!!
    val env = branchToEnvMap[branch]

    archiveBaseName.set("ktor-example")
    archiveVersion.set("$tagVersion-$env") // my archiveVersion naming convention
}

tasks.shadowJar {
    dependsOn(tasks.distTar, tasks.distZip)
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClass
            )
        )
    }
}

// =============================== Postman ===============================

node {

    download.set(false)

    // Version of node to use.
    //version.set("14.16.0")

    // Version of npm to use.
    //npmVersion.set("7.6.3")

    // Version of Yarn to use.
    //yarnVersion.set("1.22.0")

    // Base URL for fetching node distributions (change if you have a mirror).
    //distBaseUrl.set("https://nodejs.org/dist")

    // Set the work directory for unpacking node
    //workDir.set(file("${project.projectDir}/nodejs/.cache/nodejs"))

    // Set the work directory for NPM
    //npmWorkDir.set(file("${project.projectDir}/nodejs/.cache/npm"))

    // Set the work directory for Yarn
    //yarnWorkDir.set(file("${project.projectDir}/nodejs/.cache/yarn"))

    // Set the work directory where node_modules should be located
    nodeProjectDir.set(file("${project.projectDir}"))

    nodeProxySettings.set(ProxySettings.SMART)

    npmInstallCommand.set("install")
}

val npmInstallPostman by tasks.register<NpmTask>("npmInstallPostman") {
    group = "postman"
    args.set(
        listOf(
            "install",
            "uuid",
            "bent",
            "request",
            "async",
            "openapi-to-postmanv2",
            "newman",
            "newman-reporter-htmlextra",
            "--save-dev"
        )
    )
}

val openApiProjectName = "club"
val openApiSchemaUrl = "http://localhost:9000/apidocs/schema/$openApiProjectName.json"
val postmanEnvironment = "localhost"

val postmanApiKey: String = System.getenv("POSTMAN_API_KEY") ?: ""

val downloadOpenApiJson by tasks.register<NodeTask>("downloadOpenApiJson") {
    group = "postman"
    script.set(file("postman/scripts/openapi-json-provider.js"))
    args.set(listOf("downloadJson", openApiProjectName, openApiSchemaUrl))
}

val openApiToPostmanCollection by tasks.register<NodeTask>("openApiToPostmanCollection") {
    group = "postman"
    script.set(file("postman/scripts/openapi-to-postman-collection.js"))
    args.set(listOf("convert", openApiProjectName))
}

val generatePostmanCollection by tasks.register<NodeTask>("generatePostmanCollection") {
    group = "postman"
    script.set(file("postman/scripts/openapi-to-postman-collection.js"))
    args.set(listOf("downloadThenConvert", openApiProjectName))
}

val runPostmanTest by tasks.register<NodeTask>("runPostmanTest") {
    group = "postman"
    script.set(file("postman/scripts/postman-test-runner.js"))
    args.set(listOf(openApiProjectName, postmanEnvironment)) // envName(required), folderName(optional)
}

val uploadToPostmanCloud by tasks.register<NodeTask>("uploadToPostmanCloud") {
    group = "postman"
    environment.set(mapOf("X-Api-Key" to postmanApiKey))
    script.set(file("postman/scripts/postman-api.js"))
    args.set(listOf("uploadAll", openApiProjectName)) // function => uploadAll, uploadEnvironments, uploadCollection
}

// =============================== Detekt ===============================

// https://detekt.github.io/detekt/gradle.html
/**
detekt {
toolVersion = "1.15.0-RC1"

failFast = false // fail build on any finding
buildUponDefaultConfig = true // preconfigure defaults
// point to your custom config defining rules to run, overwriting default behavior
//config = files("$projectDir/config/detekt/detekt.yml")
// a way of suppressing issues before introducing detekt
//baseline = file("$projectDir/config/detekt/baseline.xml")
debug = false

input = files("src")

reports {
html.enabled = true // observe findings in your browser with structure and code snippets
xml.enabled = false // checkstyle like format mainly for integrations like Jenkins
txt.enabled = false // similar to the console output, contains issue signature to manually edit baseline files
}
}

tasks {
withType<io.gitlab.arturbosch.detekt.Detekt> {
// Target version of the generated JVM bytecode. It is used for type resolution.
jvmTarget = "1.8"
failFast = false
exclude("common/redis/ktorio", "common/swagger/nielsfalk")
}
}
 **/