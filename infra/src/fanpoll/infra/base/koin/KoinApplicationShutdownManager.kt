/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.koin

import io.ktor.events.EventHandler
import io.ktor.server.application.ApplicationEnvironment
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.KoinApplicationStopPreparing

// ApplicationEvent order: Ktor ApplicationStopPreparing → KoinApplicationStopPreparing → KoinApplicationStopped → Ktor ApplicationStopping → Ktor ApplicationStopped
// shutdown execution path: ShutDownUrl.doShutdown() → io.ktor.server.netty.NettyApplicationEngine.stop()
object KoinApplicationShutdownManager {

    private val tasks: MutableList<EventHandler<KoinApplication>> = mutableListOf()

    fun register(handler: EventHandler<KoinApplication>) {
        tasks += handler
    }

    fun complete(applicationEnvironment: ApplicationEnvironment) {
        tasks.asReversed().forEach {
            applicationEnvironment.monitor.subscribe(KoinApplicationStopPreparing, it)
        }
    }
}