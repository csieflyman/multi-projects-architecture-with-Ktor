/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.base.datetime.DateTimeUtils
import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Info
import fanpoll.infra.openapi.schema.OpenAPIObject
import fanpoll.infra.openapi.schema.Server
import fanpoll.infra.openapi.schema.component.support.BuiltinComponents
import fanpoll.infra.openapi.schema.component.support.SecurityScheme
import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject
import kotlinx.html.div
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.stream.appendHTML
import kotlinx.html.ul
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

class ProjectOpenApi(
    val projectId: String,
    private val urlRootPath: String,
    private val securitySchemes: List<SecurityScheme>,
    private val configure: (OpenAPIObject.() -> Unit)? = null
) {

    lateinit var openAPIObject: OpenAPIObject
        private set

    fun init(config: OpenApiConfig) {
        openAPIObject = OpenAPIObject(
            info = Info(
                title = "$projectId API (${config.server.env})", description = buildInfoDescription(config),
                version = config.appInfo.git.tag
            ),
            servers = listOf(Server(url = urlRootPath)),
            tags = operations.flatMap { it.tags }.toSet().toMutableList()
        )
        configure?.invoke(openAPIObject)

        securitySchemes.forEach { openAPIObject.components.add(it) }

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
                    li { +"Build Time: ${config.appInfo.buildTime}" }
                    li { +"Git Branch: ${config.appInfo.git.branch}" }
                    li { +"Git Tag Name: ${config.appInfo.git.tagName}" }
                    li { +"Git CommitId: ${config.appInfo.git.commitId}" }
                }
            }
        }
    }

    fun addModuleOpenApi(moduleOpenApi: Any) {
        addOperations(moduleOpenApi)
        addComponents(moduleOpenApi)
    }

    private val operationType = typeOf<RouteApiOperation>()
    private val operations: MutableList<RouteApiOperation> = mutableListOf()
    private fun addOperations(moduleOpenApi: Any) {
        val allOperations = moduleOpenApi.javaClass.kotlin.memberProperties
            .filter { it.returnType == operationType }
            .map { it.getter.call(moduleOpenApi) as RouteApiOperation }
        operations.addAll(allOperations)
    }

    private val componentType = typeOf<ReferenceObject>()
    val components: MutableList<ReferenceObject> = mutableListOf()
    private fun addComponents(moduleOpenApi: Any) {
        val allComponents = moduleOpenApi.javaClass.kotlin.memberProperties
            .filter { it.returnType == componentType }
            .map { it.getter.call(moduleOpenApi) as ReferenceObject }
        components.addAll(allComponents)
    }

    private fun loadReusableComponents() {
        val allComponents = mutableListOf<ReferenceObject>()
        allComponents += BuiltinComponents.components
        allComponents += components
        allComponents.forEach { openAPIObject.components.add(it) }
    }
}