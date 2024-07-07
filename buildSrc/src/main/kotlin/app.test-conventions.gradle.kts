import com.bmuschko.gradle.docker.tasks.AbstractDockerRemoteApiTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.FileInputStream
import java.util.*

plugins {
    id("app.java-conventions")
    id("com.bmuschko.docker-remote-api")
}

dependencies {
    // ========== kotlin ==========
    testImplementation(platform("org.jetbrains.kotlin:kotlin-bom:2.0.0"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // ========== kotest ==========
    testImplementation(platform("io.kotest:kotest-bom:5.9.1"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-property")
    testImplementation("io.kotest:kotest-assertions-core")

    testImplementation("io.kotest.extensions:kotest-assertions-ktor:2.0.0")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
    testImplementation("io.kotest.extensions:kotest-extensions-koin:1.3.0")

    // ========== ktor ==========
    testImplementation(platform("io.ktor:ktor-bom:2.3.12"))
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-logging")
    testImplementation("io.ktor:ktor-client-serialization")
    testImplementation("io.ktor:ktor-client-content-negotiation")

    // ========== koin ==========
    testImplementation("io.insert-koin:koin-test-junit5:3.5.6")

    // ========== testcontainers ==========
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.8"))
    testImplementation("org.testcontainers:postgresql")
}

val removeTestContainers by tasks.register<RemoveTestContainersTask>("removeTestContainers")

open class RemoveTestContainersTask : AbstractDockerRemoteApiTask() {

    override fun runRemoteCommand() {
        val containers = dockerClient.listContainersCmd()
            .withLabelFilter(mapOf("org.testcontainers" to "true", "project" to project.name))
            .exec()
        containers.forEach { container ->
            println("remove container ${container.id} from image ${container.image}")
            dockerClient.removeContainerCmd(container.id).withRemoveVolumes(true).withForce(true).exec()
        }
    }
}

tasks.withType<Test> {
    // dryRun = true

    useJUnitPlatform {}

    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }

    val env = "local-test"
    val properties = loadProperties("$env.properties")

    // https://docs.gradle.org/current/userguide/performance.html#parallel_test_execution
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    minHeapSize = "256m"
    maxHeapSize = "512m"

    failFast = true
    ignoreFailures = false
    //testLogging.showStandardStreams = true

    var reportJunitXmlEnabled = true
    var reportJunitHtmlEnabled = true

    if (properties != null) {
        val gradleProps = properties.filter { it.key.startsWith("gradle.") }
            .mapKeys { it.key.substring("gradle.".length) }.toMutableMap()
        //println("========== Testing Gradle Properties ==========")
        //println(gradleProps)

        gradleProps["minHeapSize"]?.takeIf { it.isNotEmpty() }?.also { minHeapSize = it }
        gradleProps["maxHeapSize"]?.takeIf { it.isNotEmpty() }?.also { maxHeapSize = it }
        gradleProps["failFast"]?.takeIf { it.isNotEmpty() }?.also { failFast = it.toBoolean() }
        gradleProps["ignoreFailures"]?.takeIf { it.isNotEmpty() }?.also { ignoreFailures = it.toBoolean() }
        gradleProps["report.junit.xml"]?.takeIf { it.isNotEmpty() }?.also { reportJunitXmlEnabled = it.toBoolean() }
        gradleProps["report.junit.html"]?.takeIf { it.isNotEmpty() }?.also { reportJunitHtmlEnabled = it.toBoolean() }
    }

    reports {
        junitXml.required.set(reportJunitXmlEnabled)
        html.required.set(reportJunitHtmlEnabled)
    }

    // see org.gradle.api.internal.tasks.testing.logging.DefaultTestLoggingContainer
    //testLogging {}

    doFirst {
        val appPath = "${project.rootDir.absolutePath}/app"
        val configPath = "$appPath/dist/config/$env"
        val swaggerUIPath = "$appPath/dist/swagger-ui"
        val firebaseKeyFilePath = "$configPath/firebase-key.json"

        systemProperties(
            "file.encoding" to "UTF-8",
            "user.timezone" to "UTC",
            "config.file" to "$configPath/application.conf",
            "logback.configurationFile" to "$configPath/logback.xml",
            "project.config.dir" to configPath
        )

        if (properties != null) {
            val sysProps = properties.filter { it.key.startsWith("sysProperty.") }
                .mapKeys { it.key.substring("sysProperty.".length) }
                .filterValues { it.isNotBlank() }.toMutableMap()
            println("========== Testing System Properties ==========")
            println(sysProps)

            systemProperties(sysProps)

            val envProps = properties.filter { it.key.startsWith("env.") }
                .mapKeys { it.key.substring("env.".length) }.toMutableMap()
            println("========== Testing Environment Variables ==========")
            println(envProps)

            if (envProps["SWAGGER_UI_PATH"].isNullOrBlank())
                envProps["SWAGGER_UI_PATH"] = swaggerUIPath
            if (envProps["GOOGLE_APPLICATION_CREDENTIALS"].isNullOrBlank())
                envProps["GOOGLE_APPLICATION_CREDENTIALS"] = firebaseKeyFilePath
            environment(envProps)
        }
    }

    finalizedBy(removeTestContainers)
}

fun loadProperties(path: String, keyPrefix: String? = null): Map<String, String>? {
    val propertiesFilePath = "${project.rootDir.absolutePath}/$path"
    val properties = File(propertiesFilePath).takeIf { it.exists() }
        ?.let { file -> Properties().also { props -> FileInputStream(file).use { props.load(it) } } }
        ?.map { it.key as String to it.value as String }?.toMap()
    return if (properties != null && keyPrefix != null)
        properties.filter { it.key.startsWith(keyPrefix) }.mapKeys { it.key.substring(keyPrefix.length) }.toMutableMap()
    else properties
}