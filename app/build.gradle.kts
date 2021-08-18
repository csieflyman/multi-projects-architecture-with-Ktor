import com.github.gradle.node.npm.proxy.ProxySettings
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask
import org.apache.tools.ant.filters.ReplaceTokens
import org.unbrokendome.gradle.plugins.gitversion.core.RuleContext
import org.unbrokendome.gradle.plugins.gitversion.version.SemVersion
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    application
    id("com.github.johnrengelman.shadow")
    id("org.flywaydb.flyway") version "7.11.4"
    id("org.unbroken-dome.gitversion") version "0.10.0"
    //id("org.barfuin.gradle.taskinfo") version "1.3.0"
    id("com.github.node-gradle.node") version "3.0.1"
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
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
    // -Xms128m -Xmx256m
    // -Duser.timezone=UTC -Dkotlinx.coroutines.debug
    // -Dconfig.file=$APP_HOME/application.conf -Dlogback.configurationFile=$APP_HOME/logback.xml
    // -Dproject.config.dir=$APP_HOME -Dswagger-ui.dir=$APP_HOME/swagger-ui
}

// =============================== Flyway Plugin ===============================

flyway {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/fanpoll"
    user = "fanpoll"
    password = "fanpoll"
    defaultSchema = "fanpoll"
}

// =============================== Git Plugin ===============================
// https://github.com/unbroken-dome/gradle-gitversion-plugin

tasks.determineGitVersion {
    outputs.upToDateWhen { false }
}

// my git branch naming convention
val devBranchName = "dev"
val testBranchName = "test"
val releaseBranchName = "main"
val branchToEnvMap: Map<String, String> = mapOf(
    devBranchName to "dev",
    testBranchName to "test",
    releaseBranchName to "prod"
)

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
val semVersion = (project.version as SemVersion)
val tagVersion = "${semVersion.major}.${semVersion.minor}.${semVersion.patch}"
val branch = semVersion.prereleaseTag!!
val env = branchToEnvMap[branch]

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
    archiveBaseName.set(appName)
    archiveVersion.set(tagVersion)
    archiveClassifier.set(env)

    mergeServiceFiles()
}

tasks.installShadowDist {
    dependsOn(shadowDistConfigFiles)
}

tasks.shadowDistZip {
    dependsOn(shadowDistConfigFiles)
    outputs.upToDateWhen { false }
    archiveBaseName.set(appName)
    archiveVersion.set(tagVersion)
    archiveClassifier.set(env)
}

val shadowDistConfigFiles by tasks.register("shadowDistConfigFiles") {
    group = "distribution"

    doLast {
        println("========================================")
        println("semVersion = $semVersion")
        println("env = $env")
        println("========================================")

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
val openApiSchemaUrl = "http://localhost:8080/apidocs/schema/$openApiProjectName.json"
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