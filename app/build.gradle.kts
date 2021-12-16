import com.github.gradle.node.npm.proxy.ProxySettings
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask
import org.apache.tools.ant.filters.ReplaceTokens
import org.unbrokendome.gradle.plugins.gitversion.version.SemVersion
import java.io.FileInputStream
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    application
    id("com.github.johnrengelman.shadow")
    id("org.unbroken-dome.gitversion") version "0.10.0"
    id("com.github.node-gradle.node") version "3.1.0"
    //id("org.flywaydb.flyway") version "8.2.0"
    //id("org.barfuin.gradle.taskinfo") version "1.3.0"
}

val appName = "app"
group = "com.fanpoll"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":infra", configuration = "shadow"))
    implementation(project(":projects:ops", configuration = "shadow"))
    implementation(project(":projects:club", configuration = "shadow"))
}

application {
    mainClass.set("fanpoll.infra.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Duser.timezone=UTC")
}

val semVersion = determineVersion()
val tagVersion = semVersion.tagVersion()
val env = semVersion.env()

// =============================== Shadow ===============================

sourceSets["main"].resources.srcDirs("resources")

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClass
            )
        )
    }
}

tasks.shadowJar {
    mergeServiceFiles()

    archiveBaseName.set(appName)
    archiveVersion.set(tagVersion)
    archiveClassifier.set(env)

    doFirst {
        // Don't need to copy swagger-ui every build
        delete(fileTree("src/dist").matching {
            exclude("swagger-ui/**")
        })

        if (!Paths.get("src/dist/swagger-ui").toFile().exists()) {
            copy {
                from("dist/swagger-ui")
                into("src/dist/swagger-ui")
            }
        }

        copy {
            from("dist/config/$env")
            into("src/dist")
            filter<ReplaceTokens>(
                "tokens" to mapOf(
                    "env" to env,
                    "gitTagVersion" to tagVersion,
                    "gitCommitVersion" to semVersion.toString(),
                    "buildTime" to DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
                )
            )
        }

        copy {
            from("dist/bin")
            into("src/dist/bin")
        }
    }
}

tasks.shadowDistZip {
    outputs.upToDateWhen { false }

    archiveBaseName.set(appName)
    archiveVersion.set(tagVersion)
    archiveClassifier.set(env)
}

tasks.runShadow {

    minHeapSize = "128m"
    maxHeapSize = "256m"

    doFirst {
        val appPath = "${project.rootDir.absolutePath}/app"
        val configPath = "$appPath/src/dist"
        val swaggerUIPath = "$appPath/src/dist/swagger-ui"
        val firebaseKeyFilePath = "$configPath/firebase-key.json"

        systemProperties(
            "file.encoding" to "UTF-8",
            "user.timezone" to "UTC",
            "config.file" to "$configPath/application.conf",
            "logback.configurationFile" to "$configPath/logback.xml",
            "project.config.dir" to configPath
        )

        val env = "local-dev"
        val properties = loadProperties("$env.properties")
        if (properties != null) {
            val gradleProps = properties.filter { it.key.startsWith("gradle.") }
                .mapKeys { it.key.substring("gradle.".length) }
                .filterValues { it.isNotBlank() }.toMutableMap()
            println("========== Dev Gradle Properties ==========")
            println(gradleProps)

            gradleProps["run.minHeapSize"]?.takeIf { it.isNotEmpty() }?.also { minHeapSize = it }
            gradleProps["run.maxHeapSize"]?.takeIf { it.isNotEmpty() }?.also { maxHeapSize = it }

            val envProps = properties.filter { it.key.startsWith("env.") }
                .mapKeys { it.key.substring("env.".length) }.toMutableMap()
            println("========== Dev Environment variables ==========")
            println(envProps)

            if (envProps["SWAGGER_UI_PATH"].isNullOrBlank())
                envProps["SWAGGER_UI_PATH"] = swaggerUIPath
            if (envProps["GOOGLE_APPLICATION_CREDENTIALS"].isNullOrBlank())
                envProps["GOOGLE_APPLICATION_CREDENTIALS"] = firebaseKeyFilePath
            environment(envProps)
        }
    }
}

// =============================== Utilities ===============================
fun loadProperties(path: String, keyPrefix: String? = null): Map<String, String>? {
    val propertiesFilePath = "${project.rootDir.absolutePath}/$path"
    val properties = File(propertiesFilePath).takeIf { it.exists() }
        ?.let { file -> Properties().also { props -> FileInputStream(file).use { props.load(it) } } }
        ?.map { it.key as String to it.value as String }?.toMap()
    return if (properties != null && keyPrefix != null)
        properties.filter { it.key.startsWith(keyPrefix) }.mapKeys { it.key.substring(keyPrefix.length) }.toMutableMap()
    else properties
}

// =============================== Git Plugin ===============================
// https://github.com/unbroken-dome/gradle-gitversion-plugin
// base on my git branch/tag naming convention
fun determineVersion(): SemVersion {
    if (project.version is SemVersion)
        return project.version as SemVersion

    val branchEnvProps = loadProperties("branch-env.properties") ?: error("branch-env.properties is missing")
    val releaseBranch = branchEnvProps["releaseBranch"] ?: error("releaseBranch should be specified")
    val branchToEnvMap = branchEnvProps.filter { it.key.startsWith("branch.") }
        .mapKeys { it.key.substring("branch.".length) }.toMutableMap()

    gitVersion {
        rules {
            val branchPatternStrings = branchToEnvMap.keys.toList()
            branchPatternStrings.forEach { patternStr ->
                onBranch(patternStr.toPattern()) {
                    val currentBranch = branchName!!
                    val tagPrefix = if (currentBranch == releaseBranch) "" else "$currentBranch-"
                    val tag = findLatestTag("""${tagPrefix}(\d+)\.(\d+)\.(\d+)""".toPattern())
                    val versionNumber = tag?.matches?.toList()?.apply { subList(1, size) }?.map { it.toInt() } ?: listOf(1, 0, 0)
                    val preReleaseTag =
                        (tag?.let { "${countCommitsSince(it)}-${it.commit.id}" } ?: repository.head!!.id) + "_$currentBranch"
                    val env = branchToEnvMap[patternStr]!!
                    version.set(versionNumber[0], versionNumber[1], versionNumber[2], preReleaseTag, env)
                    println("========== determineVersion ==========")
                    println("semVersion = $version")
                    println("tagVersion = ${version.tagVersion()}")
                    println("currentBranch = ${version.currentBranch()}")
                    println("env = ${version.env()}")
                }
            }
        }
    }

    tasks.determineGitVersion {
        outputs.upToDateWhen { false }
    }

    project.version = gitVersion.determineVersion()
    return project.version as SemVersion
}

fun SemVersion.tagVersion(): String = "${major}.${minor}.${patch}"
fun SemVersion.currentBranch(): String = prereleaseTag!!.substringAfter("_")
fun SemVersion.env(): String = buildMetadata!!

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

val openApiProjectName = "ops"
val openApiSchemaUrl = "http://localhost:8080/apidocs/schema/$openApiProjectName.json"
val postmanEnvironment = "localhost"
val swaggerUserName = "fanpoll"
val swaggerPassword = "fanpoll"

val postmanApiKey: String = System.getenv("POSTMAN_API_KEY") ?: ""

val downloadOpenApiJson by tasks.register<NodeTask>("downloadOpenApiJson") {
    group = "postman"
    script.set(file("postman/scripts/openapi-json-provider.js"))
    args.set(listOf(openApiProjectName, openApiSchemaUrl, swaggerUserName, swaggerPassword))
}

val openApiToPostmanCollection by tasks.register<NodeTask>("openApiToPostmanCollection") {
    group = "postman"
    script.set(file("postman/scripts/openapi-to-postman-collection.js"))
    args.set(listOf(openApiProjectName))
}

val generatePostmanCollection by tasks.register("generatePostmanCollection") {
    dependsOn(downloadOpenApiJson, openApiToPostmanCollection)
    group = "postman"
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