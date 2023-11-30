/*
 * Copyright (c) 2023. fanpoll All rights reserved.
 */

import com.github.gradle.node.npm.proxy.ProxySettings
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask

plugins {
    id("com.github.node-gradle.node") version "7.0.0"
}

// ========== Configuration ==========

val openApiProjectName = "club" // club, ops
val openApiSchemaUrl = "http://localhost:8080/apidocs/schema/$openApiProjectName.json"
val postmanEnvironment = "localhost"
val swaggerUserName = "fanpoll"
val swaggerPassword = "fanpoll"

val postmanApiKey: String = System.getenv("POSTMAN_API_KEY") ?: "Your Api Key"

// ========== Tasks ==========

val npmInstallPostman by tasks.register<NpmTask>("npmInstallPostman") {
    group = "scripts"
    args.set(
        listOf(
            "install",
            "uuid",
            "axios",
            "async",
            "openapi-to-postmanv2",
            "newman",
            "newman-reporter-htmlextra",
            "--legacy-peer-deps",
            "--save-dev"
        )
    )
}

val downloadOpenApiJson by tasks.register<NodeTask>("downloadOpenApiJson") {
    group = "scripts"
    script.set(file("scripts/openapi-json-provider.js"))
    args.set(listOf(openApiProjectName, openApiSchemaUrl, swaggerUserName, swaggerPassword))
}

val openApiToPostmanCollection by tasks.register<NodeTask>("openApiToPostmanCollection") {
    group = "scripts"
    script.set(file("scripts/openapi-to-postman-collection.js"))
    args.set(listOf(openApiProjectName))
}

val generatePostmanCollection by tasks.register("generatePostmanCollection") {
    dependsOn(downloadOpenApiJson, openApiToPostmanCollection)
    group = "scripts"
}

val runPostmanTest by tasks.register<NodeTask>("runPostmanTest") {
    group = "scripts"
    script.set(file("scripts/postman-test-runner.js"))
    args.set(listOf(openApiProjectName, postmanEnvironment)) // envName(required), folderName(optional)
}

val uploadToPostmanCloud by tasks.register<NodeTask>("uploadToPostmanCloud") {
    group = "scripts"
    environment.set(mapOf("X-Api-Key" to postmanApiKey))
    script.set(file("scripts/postman-api.js"))
    args.set(listOf("uploadAll", openApiProjectName)) // function => uploadAll, uploadEnvironments, uploadCollection
}

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