/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.utils.Jackson
import kotlin.collections.set

object ProjectOpenApiManager {

    private lateinit var config: OpenApiConfig

    private val openApiMap: MutableMap<String, ProjectOpenApi> = mutableMapOf()
    private val openApiJsonMap: MutableMap<String, String> = mutableMapOf()

    fun init(config: OpenApiConfig) {
        this.config = config
    }

    fun registerProject(projectOpenApi: ProjectOpenApi) {
        val projectId = projectOpenApi.projectId
        require(!openApiMap.containsKey(projectId))
        openApiMap[projectId] = projectOpenApi

        projectOpenApi.init(config)
    }

    fun getProjectIds(): Set<String> {
        return openApiMap.keys.toSet()
    }

    fun getOpenApiJson(projectId: String): String {
        return openApiJsonMap.getOrPut(projectId) {
            val openApiRoot = openApiMap[projectId]?.openAPIObject
                ?: throw RequestException(ResponseCode.ENTITY_NOT_FOUND, "$projectId openapi json is not exist")
            Jackson.toJsonString(openApiRoot)
        }
    }
}