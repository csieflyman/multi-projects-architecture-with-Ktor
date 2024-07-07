/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app

import fanpoll.infra.release.app.repository.exposed.AppReleaseExposedRepository
import fanpoll.infra.release.app.service.AppReleaseChecker
import fanpoll.infra.release.app.service.AppReleaseService
import io.ktor.server.application.Application
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

fun Application.loadAppReleaseModule() = loadKoinModules(module(createdAtStart = true) {
    val appReleaseRepository = AppReleaseExposedRepository()
    val appReleaseService = AppReleaseService(appReleaseRepository)
    val appReleaseChecker = AppReleaseChecker(appReleaseRepository)
    single { appReleaseRepository }
    single { appReleaseService }
    single { appReleaseChecker }
})