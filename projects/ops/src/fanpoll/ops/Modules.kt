/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.database.hikari.HikariDataSourceManager
import fanpoll.infra.database.jasync.JasyncConnectionPoolManager
import fanpoll.infra.koin.KoinApplicationShutdownManager
import fanpoll.ops.auth.loadAuthModule
import fanpoll.ops.database.loadOpsDatabaseModule
import fanpoll.ops.log.loadLogModule
import fanpoll.ops.monitor.loadMonitorModule
import fanpoll.ops.notification.loadNotificationModule
import fanpoll.ops.release.loadReleaseModule
import fanpoll.ops.report.loadReportModule
import fanpoll.ops.user.loadUserModule
import io.ktor.server.application.Application
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.dsl.koinApplication

fun Application.loadProjectModules(config: OpsConfig) {

    OpsKoinContext.init()
    loadBaseModule()
    loadOpsDatabaseModule(config.databases)
    loadUserModule()
    loadAuthModule(config.auth)
    loadNotificationModule()
    loadReportModule()
    loadReleaseModule()
    loadMonitorModule()
    loadLogModule()

    KoinApplicationShutdownManager.register {
        JasyncConnectionPoolManager.closeAllConnectionPools()
        HikariDataSourceManager.closeAllDataSources()
    }
}

object OpsKoinContext {

    lateinit var koin: Koin
    fun init() {
        koin = koinApplication {}.koin
    }
}

abstract class OpsKoinComponent : KoinComponent {
    final override fun getKoin(): Koin = OpsKoinContext.koin
    fun getInfraKoin(): Koin = super.getKoin()
}