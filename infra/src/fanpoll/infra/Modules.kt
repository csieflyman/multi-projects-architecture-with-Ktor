/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.infra.auth.loadAuthModule
import fanpoll.infra.cache.loadCacheModule
import fanpoll.infra.config.MyApplicationConfig
import fanpoll.infra.database.loadInfraDatabaseModule
import fanpoll.infra.i18n.loadI18nModule
import fanpoll.infra.logging.loadLoggingModule
import fanpoll.infra.notification.loadNotificationModule
import fanpoll.infra.openapi.loadOpenApiModule
import fanpoll.infra.redis.loadRedisModule
import fanpoll.infra.release.app.loadAppReleaseModule
import fanpoll.infra.report.loadReportModule
import fanpoll.infra.session.loadSessionModule
import io.ktor.server.application.Application

fun Application.loadInfraModules(appConfig: MyApplicationConfig) {
    loadInfraDatabaseModule(appConfig.infra.databases)
    loadLoggingModule(appConfig.infra.logging)
    loadI18nModule(appConfig.infra.i18n)
    loadOpenApiModule(appConfig.infra.openApi)
    loadRedisModule(appConfig.infra.redis)
    loadCacheModule(appConfig.infra.cache)
    loadSessionModule(appConfig.infra.sessionAuth)
    loadAuthModule(appConfig.infra.sessionAuth)
    loadNotificationModule(appConfig.infra.notification, appConfig.server.env)
    loadReportModule()
    loadAppReleaseModule()
}