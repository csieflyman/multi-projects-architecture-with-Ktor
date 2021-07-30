/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.auth.AuthConst
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.openapi.schema.Info
import fanpoll.infra.openapi.schema.OpenAPIObject
import fanpoll.infra.openapi.schema.Server
import fanpoll.infra.openapi.schema.component.support.BuiltinComponents
import fanpoll.infra.openapi.schema.component.support.ComponentLoader
import fanpoll.infra.openapi.schema.component.support.DefaultSecurityScheme
import fanpoll.infra.openapi.schema.component.support.SecurityScheme
import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject
import kotlinx.html.div
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.stream.appendHTML
import kotlinx.html.ul
import java.time.LocalDateTime

class ProjectOpenApi(
    val projectId: String,
    private val urlRootPath: String,
    private val securitySchemes: List<SecurityScheme>,
    private val operations: List<OpenApiOperation>,
    private val componentLoader: ComponentLoader? = null,
    private val configure: (OpenAPIObject.() -> Unit)? = null
) {

    lateinit var openAPIObject: OpenAPIObject
        private set

    fun init(config: OpenApiConfig) {
        openAPIObject = OpenAPIObject(
            info = Info(
                title = "$projectId API (${config.info.env})", description = buildInfoDescription(config),
                version = config.info.gitTagVersion
            ),
            servers = listOf(Server(url = urlRootPath)),
            tags = operations.flatMap { it.tags }
        )
        configure?.invoke(openAPIObject)

        openAPIObject.initComponents(securitySchemes)
        loadReusableComponents()

        operations.forEach { it.init(openAPIObject) }
    }

    private fun buildInfoDescription(config: OpenApiConfig): String {
        return buildString {
            appendHTML(false).div {
                p {
                    +config.info.description
                }
                ul {
                    li {
                        +"Server Start Time: ${
                            DateTimeUtils.LOCAL_DATE_TIME_FORMATTER.format(LocalDateTime.now(DateTimeUtils.TAIWAN_ZONE_ID))
                        }"
                    }
                    li { +"Build Time: ${config.info.buildTime}" }
                    li { +"Git Commit Version: ${config.info.gitCommitVersion}" }
                }
            }
        }
    }

    private fun loadReusableComponents() {
        val components = mutableListOf<ReferenceObject>()
        components += BuiltinComponents.load()
        componentLoader?.load()?.also { components += it }
        components.forEach { openAPIObject.components.add(it) }
    }

    companion object {

        val apiKeySecurityScheme = SecurityScheme.apiKeyAuth(
            DefaultSecurityScheme.ApiKeyAuth.name,
            AuthConst.API_KEY_HEADER_NAME
        )
    }
}