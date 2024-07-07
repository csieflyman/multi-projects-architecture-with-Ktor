/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.release

import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import fanpoll.infra.release.app.domain.AppRelease
import fanpoll.infra.release.app.dtos.AppReleaseDTO
import fanpoll.infra.release.app.repository.exposed.AppReleaseTable
import io.ktor.server.application.Application

fun Application.loadReleaseModule() {
    registerResultRowMappers()
}

private fun registerResultRowMappers() {
    ResultRowMappers.register(
        ResultRowMapper(AppRelease::class, AppReleaseTable),
        ResultRowMapper(AppReleaseDTO::class, AppReleaseTable)
    )
}