/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.release.app.routes

import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag

object AppReleaseOpenApi {

    private val AppReleaseTag = Tag("appRelease")

    val CreateAppRelease = RouteApiOperation("CreateAppRelease", listOf(AppReleaseTag))
    val UpdateAppRelease = RouteApiOperation("UpdateAppRelease", listOf(AppReleaseTag))
    val FindAppReleases = RouteApiOperation("FindAppReleases", listOf(AppReleaseTag))
    val CheckAppRelease = RouteApiOperation("CheckAppRelease", listOf(AppReleaseTag))
}