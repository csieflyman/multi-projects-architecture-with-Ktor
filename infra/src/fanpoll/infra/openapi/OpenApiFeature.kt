/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.openapi.schema.operation.definitions.PropertyDef
import fanpoll.infra.openapi.schema.operation.definitions.SchemaDataType
import fanpoll.infra.openapi.schema.operation.support.converters.SchemaObjectConverter
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.http.content.files
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

class OpenApiFeature(configuration: Configuration) {

    class Configuration {

        private lateinit var openApiInfoConfig: OpenApiInfoConfig
        private var swaggerUIConfig: SwaggerUIConfig? = null

        fun openApiInfo(block: OpenApiInfoConfig.Builder.() -> Unit) {
            openApiInfoConfig = OpenApiInfoConfig.Builder().apply(block).build()
        }

        fun swaggerUI(configure: SwaggerUIConfig.Builder.() -> Unit) {
            swaggerUIConfig = SwaggerUIConfig.Builder().apply(configure).build()
        }

        fun build(): OpenApiConfig {
            return OpenApiConfig(openApiInfoConfig, swaggerUIConfig)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, OpenApiFeature> {

        override val key = AttributeKey<OpenApiFeature>("OpenApi")

        private val logger = KotlinLogging.logger {}

        private const val swaggerUIAuthProviderName = "swaggerUI"

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): OpenApiFeature {
            val configuration = Configuration().apply(configure)
            val feature = OpenApiFeature(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            val openApiConfig = appConfig.infra.openApi ?: configuration.build()
            val projectOpenApiManager = ProjectOpenApiManager(openApiConfig)

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single { projectOpenApiManager }
                    }
                )
            }

            val swaggerUIConfig = openApiConfig.swaggerUI
            val swaggerUIAuth = swaggerUIConfig?.needAuth ?: false
            if (swaggerUIAuth) {
                requireNotNull(swaggerUIConfig)
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
                if (swaggerUIAuth) {
                    authenticate(swaggerUIAuthProviderName) {
                        apiDocsRoute(this, swaggerUIConfig, projectOpenApiManager)
                    }
                } else {
                    apiDocsRoute(this, swaggerUIConfig, projectOpenApiManager)
                }
            }

            registerPropertyConverters()

            return feature
        }

        private fun apiDocsRoute(route: Route, config: SwaggerUIConfig?, projectOpenApiManager: ProjectOpenApiManager) {

            val swaggerUiDir = System.getProperty("swagger-ui.dir") ?: config?.dir
            ?: throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR, "application system property: -Dswagger-ui.dir is missing"
            )
            logger.info { "-Dswaggerui.dir = $swaggerUiDir" }

            route {

                get("/apidocs/schema/{schemaJsonFileName}") {
                    val schemaJsonFileName = call.parameters["schemaJsonFileName"]
                        ?: throw RequestException(InfraResponseCode.BAD_REQUEST_PATH, "schema json file name is required")
                    val projectId = schemaJsonFileName.substringBefore(".")
                    call.respond(projectOpenApiManager.getOpenApiJson(projectId))
                }

                get("/apidocs") {
                    call.respondRedirect("/apidocs/index.html")
                }

                static("apidocs") {
                    resources("swagger-ui")
                    files(swaggerUiDir)
                }
            }
        }

        private fun registerPropertyConverters() {
            SchemaObjectConverter.registerPropertyConverter(Lang::class) {
                PropertyDef(
                    "lang", SchemaDataType.string, description = "LanguageTag",
                    kClass = Lang::class
                )
            }
        }
    }
}

data class OpenApiConfig(
    val info: OpenApiInfoConfig,
    val swaggerUI: SwaggerUIConfig? = null
)

data class OpenApiInfoConfig(
    val env: String,
    val gitTagVersion: String,
    val gitCommitVersion: String,
    val buildTime: String,
    val description: String = ""
) {

    class Builder {

        lateinit var env: String
        lateinit var gitTagVersion: String
        lateinit var gitCommitVersion: String
        lateinit var buildTime: String
        var description: String = ""

        fun build(): OpenApiInfoConfig {
            return OpenApiInfoConfig(env, gitTagVersion, gitCommitVersion, buildTime, description)
        }
    }
}

data class SwaggerUIConfig(
    val dir: String?,
    val username: String?,
    val password: String?
) {

    val needAuth: Boolean = username != null && password != null

    class Builder {

        var dir: String? = null
        var username: String? = null
        var password: String? = null

        fun build(): SwaggerUIConfig {
            return SwaggerUIConfig(dir, username, password)
        }
    }
}