/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.openapi.OpenApiOperation
import fanpoll.infra.openapi.schema.Tag
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

object OpsOpenApiOperations {

    private val MonitorTag = Tag("monitor")
    private val AppReleaseTag = Tag("appRelease", "App發佈")

    val HealthCheck = OpenApiOperation("HealthCheck", listOf(MonitorTag))

    val CreateAppRelease = OpenApiOperation("CreateAppRelease", listOf(AppReleaseTag))
    val UpdateAppRelease = OpenApiOperation("UpdateAppRelease", listOf(AppReleaseTag))
    val FindAppReleases = OpenApiOperation("FindAppReleases", listOf(AppReleaseTag))
    val CheckAppRelease = OpenApiOperation("CheckAppRelease", listOf(AppReleaseTag))

    private val routeType = typeOf<OpenApiOperation>()

    fun all(): List<OpenApiOperation> = OpsOpenApiOperations::class.memberProperties
        .filter { it.returnType == routeType }
        .map { it.getter.call(this) as OpenApiOperation }
}

