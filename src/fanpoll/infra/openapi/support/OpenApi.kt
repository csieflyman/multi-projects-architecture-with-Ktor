/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.support

import fanpoll.infra.auth.ServiceAuth
import fanpoll.infra.openapi.OpenApiConfig
import fanpoll.infra.openapi.definition.*
import fanpoll.infra.utils.DateTimeUtils
import kotlinx.html.div
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.stream.appendHTML
import kotlinx.html.ul
import java.time.LocalDateTime

class OpenApi(
    val projectId: String,
    private val urlRootPath: String,
    private val securitySchemes: List<SecurityScheme>,
    private val operations: List<OpenApiRoute>,
    private val projectReusableComponents: ReusableComponents? = null,
    private val configure: (Root.() -> Unit)? = null
) {

    lateinit var root: Root
        private set

    fun init(config: OpenApiConfig) {
        root = Root(
            info = Information(
                title = "$projectId API (${config.info.env})", description = buildInfoDescription(config),
                version = config.info.gitTagVersion
            ),
            servers = listOf(Server(url = urlRootPath)),
            tags = operations.flatMap { it.tags }.toSet()
        )
        configure?.invoke(root)

        root.initComponents(securitySchemes)
        loadReusableComponents()

        operations.forEach { it.init(this) }
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
        val reusableComponents = mutableListOf<ReferenceObject>()
        reusableComponents += DefaultReusableComponents.loadReferenceObjects()
        projectReusableComponents?.loadReferenceObjects()?.also { reusableComponents += it }
        reusableComponents.forEach { root.components.reuse(it) }
    }

    companion object {

        val apiKeySecurityScheme = SecurityScheme.apiKeyAuth(
            DefaultSecurityScheme.ApiKeyAuth.name,
            ServiceAuth.API_KEY_HEADER_NAME
        )
    }
}