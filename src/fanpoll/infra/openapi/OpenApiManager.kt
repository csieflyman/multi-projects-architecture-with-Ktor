/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.openapi.definition.Root
import fanpoll.infra.openapi.support.OpenApi
import fanpoll.infra.openapi.support.OpenApiRoute
import fanpoll.infra.utils.Jackson
import io.ktor.http.HttpMethod
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KType

object OpenApiManager {

    private lateinit var config: OpenApiConfig

    private val projectOpenApis: MutableMap<String, OpenApi> = mutableMapOf()
    private val projectSchemaJson: MutableMap<String, String> = mutableMapOf()

    fun init(config: OpenApiConfig) {
        OpenApiManager.config = config
    }

    fun registerProject(openApi: OpenApi) {
        val projectId = openApi.projectId
        require(!projectOpenApis.containsKey(projectId))
        projectOpenApis[projectId] = openApi

        openApi.init(config)
    }

    fun getProjectIds(): Set<String> {
        return projectOpenApis.keys.toSet()
    }

    fun getProjectSchemaJson(projectId: String): String {
        return projectSchemaJson.getOrPut(projectId) { Jackson.toJsonString(getProjectSchema(projectId)) }
    }

    private fun getProjectSchema(projectId: String): Root {
        return projectOpenApis[projectId]?.root
            ?: throw RequestException(ResponseCode.ENTITY_NOT_FOUND, "$projectId schema is not exist")
    }

    fun bindRoute(
        routeAuths: List<PrincipalAuth>?, routePath: String, method: HttpMethod, openApiRoute: OpenApiRoute,
        requestBodyType: KType, responseBodyType: KType,
        locationClass: KClass<*>? = null
    ) {
        openApiRoute.bindAuth(routeAuths)
        openApiRoute.bindOperation(locationClass, requestBodyType, responseBodyType)
        openApiRoute.bindPath(routePath, method, locationClass)
    }
}