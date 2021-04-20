/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.http.content.files
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.routing
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun Application.swaggerUI(config: SwaggerUIConfig) {

    routing {
        if (config.needAuth) {
            authenticate(SwaggerUIConfig.authProviderName) {
                apiDocsRoute(this, config)
            }
        } else {
            apiDocsRoute(this, config)
        }
    }
}

private fun apiDocsRoute(route: Route, config: SwaggerUIConfig) {

    val swaggerUiDir = System.getProperty("swagger-ui.dir")
        ?: config.dir ?: throw InternalServerErrorException(
            ResponseCode.SERVER_CONFIG_ERROR,
            "application system property: -Dswagger-ui.dir is missing"
        )
    logger.info { "-Dswaggerui.dir = $swaggerUiDir" }

    route {

        get("/apidocs/schema/{schemaJsonFileName}") {
            val schemaJsonFileName = call.parameters["schemaJsonFileName"]
                ?: throw RequestException(ResponseCode.REQUEST_BAD_PATH, "schema json file name is required")
            val projectId = schemaJsonFileName.substringBefore(".")
            call.respond(OpenApiManager.getProjectSchemaJson(projectId))
        }

        get("/apidocs") {
            call.respondRedirect("/apidocs/index.html")
        }

        static("apidocs") {
            resource("index.html", "swagger-ui/index.html")
            OpenApiManager.getProjectIds().forEach { projectId ->
                resource("$projectId.html", "swagger-ui/$projectId.html")
            }
            files(swaggerUiDir)
        }
    }
}

fun Authentication.Configuration.swaggerUI(config: SwaggerUIConfig) {

    basic(SwaggerUIConfig.authProviderName) {
        realm = SwaggerUIConfig.authProviderName
        validate { userPasswordCredential ->
            if (config.username == userPasswordCredential.name &&
                config.password == userPasswordCredential.password
            ) UserIdPrincipal(userPasswordCredential.name) else null
        }
    }
}