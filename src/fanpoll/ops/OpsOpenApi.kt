/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.openapi.definition.Tag
import fanpoll.infra.openapi.support.OpenApiRoute
import fanpoll.infra.openapi.support.OpenApiRouteSupport
import kotlin.reflect.full.memberProperties

object OpsOpenApiRoutes {

    private val MonitorTag = Tag("monitor")
    private val AppReleaseTag = Tag("appRelease", "App發佈")

    val HealthCheck = OpenApiRoute("HealthCheck", listOf(MonitorTag))

    val CreateAppRelease = OpenApiRoute("CreateAppRelease", listOf(AppReleaseTag))
    val UpdateAppRelease = OpenApiRoute("UpdateAppRelease", listOf(AppReleaseTag))
    val FindAppReleases = OpenApiRoute("FindAppReleases", listOf(AppReleaseTag))
    val CheckAppRelease = OpenApiRoute("CheckAppRelease", listOf(AppReleaseTag))

    fun all(): List<OpenApiRoute> = OpsOpenApiRoutes::class.memberProperties
        .filter { it.returnType == OpenApiRouteSupport.routeType }
        .map { it.getter.call(this) as OpenApiRoute }
}

