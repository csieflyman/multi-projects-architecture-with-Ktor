/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.json.Jackson
import fanpoll.infra.base.response.ResponseCode
import kotlin.collections.set

class ProjectOpenApiManager(val config: OpenApiConfig) {

    private val openApiMap: MutableMap<String, ProjectOpenApi> = mutableMapOf()
    private val openApiJsonMap: MutableMap<String, String> = mutableMapOf()

    fun register(projectOpenApi: ProjectOpenApi) {
        val projectId = projectOpenApi.projectId
        require(!openApiMap.containsKey(projectId))
        openApiMap[projectId] = projectOpenApi

        projectOpenApi.init(config)
    }

    fun getOpenApiJson(projectId: String): String {
        return openApiJsonMap.getOrPut(projectId) {
            val openAPIObject = openApiMap[projectId]?.openAPIObject
                ?: throw RequestException(ResponseCode.ENTITY_NOT_FOUND, "$projectId openapi json not found")
            Jackson.toJsonString(openAPIObject)
        }
    }
}