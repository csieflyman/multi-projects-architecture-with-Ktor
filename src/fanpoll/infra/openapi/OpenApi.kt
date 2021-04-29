/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import com.neovisionaries.i18n.LanguageCode
import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
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
import io.ktor.util.AttributeKey
import mu.KotlinLogging

class OpenApi(configuration: Configuration) {

    val config = configuration.openApiConfig

    class Configuration {

        lateinit var openApiConfig: OpenApiConfig
    }

    companion object Feature : ApplicationFeature<Application, Configuration, OpenApi> {

        override val key = AttributeKey<OpenApi>("OpenApi")

        private val logger = KotlinLogging.logger {}

        private const val swaggerUIAuthProviderName = "swaggerUI"

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): OpenApi {

            val configuration = Configuration().apply(configure)

            val feature = OpenApi(configuration)

            val swaggerUIConfig = configuration.openApiConfig.swaggerUI
            if (swaggerUIConfig.needAuth) {
                pipeline.feature(Authentication).apply {
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
            }

            pipeline.routing {
                if (swaggerUIConfig.needAuth) {
                    authenticate(swaggerUIAuthProviderName) {
                        apiDocsRoute(this, swaggerUIConfig)
                    }
                } else {
                    apiDocsRoute(this, swaggerUIConfig)
                }
            }

            return feature
        }

        private fun apiDocsRoute(route: Route, config: SwaggerUIConfig) {

            val swaggerUiDir = System.getProperty("swagger-ui.dir") ?: config.dir
            ?: throw InternalServerErrorException(
                ResponseCode.SERVER_CONFIG_ERROR, "application system property: -Dswagger-ui.dir is missing"
            )
            logger.info { "-Dswaggerui.dir = $swaggerUiDir" }

            route {

                get("/apidocs/schema/{schemaJsonFileName}") {
                    val schemaJsonFileName = call.parameters["schemaJsonFileName"]
                        ?: throw RequestException(ResponseCode.REQUEST_BAD_PATH, "schema json file name is required")
                    val projectId = schemaJsonFileName.substringBefore(".")
                    call.respond(ProjectOpenApiManager.getOpenApiJson(projectId))
                }

                get("/apidocs") {
                    call.respondRedirect("/apidocs/index.html")
                }

                static("apidocs") {
                    resource("index.html", "swagger-ui/index.html")
                    ProjectOpenApiManager.getProjectIds().forEach { projectId ->
                        resource("$projectId.html", "swagger-ui/$projectId.html")
                    }
                    files(swaggerUiDir)
                }
            }
        }
    }
}

data class OpenApiConfig(
    val info: OpenApiInfoConfig,
    val swaggerUI: SwaggerUIConfig
) {
    companion object {
        val langCode = LanguageCode.zh
    }
}

data class OpenApiInfoConfig(
    val env: String,
    val gitTagVersion: String,
    val gitCommitVersion: String,
    val buildTime: String,
    val description: String = ""
)

data class SwaggerUIConfig(
    val dir: String?,
    val username: String?,
    val password: String?
) {

    val needAuth: Boolean = username != null && password != null
}