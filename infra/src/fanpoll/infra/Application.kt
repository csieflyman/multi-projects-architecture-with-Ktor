/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.infra.config.ApplicationConfigLoader
import fanpoll.infra.config.MyApplicationConfig
import fanpoll.infra.koin.KoinApplicationShutdownManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private val logger = KotlinLogging.logger {}

fun Application.main(myAppConfig: MyApplicationConfig? = null) {
    val appConfig = myAppConfig ?: ApplicationConfigLoader.load()

    configureBasicPlugins(appConfig)

    configureKoin(appConfig)

    loadInfraModules(appConfig)

    configurePluginsAfterModulesLoaded(appConfig)

    KoinApplicationShutdownManager.complete(environment)

    staticContentRoutes()

    routing {
        logger.info {
            "========== Infra Routes ===========\n" +
                    getAllRoutes().joinToString("\n") { it.toString() }
        }
    }
}