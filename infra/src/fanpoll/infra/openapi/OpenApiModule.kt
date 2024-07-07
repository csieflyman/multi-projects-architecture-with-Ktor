/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.plugin
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.io.File

private val logger = KotlinLogging.logger {}
fun Application.loadOpenApiModule(openApiConfig: OpenApiConfig) {
    val projectOpenApiManager = ProjectOpenApiManager(openApiConfig)
    loadKoinModules(
        module(createdAtStart = true) {
            single { projectOpenApiManager }
        }
    )
    initSwagger(openApiConfig.swaggerUI, projectOpenApiManager)
}

private fun Application.initSwagger(swaggerUIConfig: SwaggerUIConfig, projectOpenApiManager: ProjectOpenApiManager) {
    if (swaggerUIConfig.authEnabled) {
        configureSwaggerRouteAuth(swaggerUIConfig, projectOpenApiManager)
    } else {
        routing {
            configureSwaggerRoute(swaggerUIConfig, projectOpenApiManager)
        }
    }
}

private fun Application.configureSwaggerRouteAuth(swaggerUIConfig: SwaggerUIConfig, projectOpenApiManager: ProjectOpenApiManager) {
    val swaggerUIAuthProviderName = "swaggerUI"
    plugin(Authentication).apply {
        configure {
            basic(swaggerUIAuthProviderName) {
                realm = swaggerUIAuthProviderName
                validate { userPasswordCredential ->
                    if (swaggerUIConfig.username == userPasswordCredential.name &&
                        swaggerUIConfig.password == userPasswordCredential.password
                    ) UserIdPrincipal(userPasswordCredential.name) else null
                }
            }
        }
    }

    routing {
        authenticate(swaggerUIAuthProviderName) {
            configureSwaggerRoute(swaggerUIConfig, projectOpenApiManager)
        }
    }
}

private fun Route.configureSwaggerRoute(config: SwaggerUIConfig, projectOpenApiManager: ProjectOpenApiManager) {
    val swaggerUiDir = System.getProperty("swagger-ui.dir") ?: config.dir
    logger.info { "-Dswaggerui.dir = $swaggerUiDir" }

    get("/apidocs/schema/{schemaJsonFileName}") {
        val schemaJsonFileName = call.parameters["schemaJsonFileName"]
            ?: throw RequestException(InfraResponseCode.BAD_REQUEST_PATH, "schema json file name is required")
        val projectId = schemaJsonFileName.substringBefore(".")
        call.respond(projectOpenApiManager.getOpenApiJson(projectId))
    }

    get("/apidocs") {
        call.respondRedirect("/apidocs/index.html")
    }

    staticResources("/apidocs", "swagger-ui")
    staticFiles("/apidocs", File(swaggerUiDir))
}