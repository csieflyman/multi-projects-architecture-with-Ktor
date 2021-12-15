/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.logging.LogDestination
import fanpoll.ops.features.*
import io.ktor.routing.Routing
import org.koin.ktor.ext.inject

fun Routing.ops() {

    val appConfig by inject<MyApplicationConfig>()

    opsUser()
    opsLogin()

    opsMonitor()
    opsDataReport()
    opsAppRelease()

    if (appConfig.infra.notification?.logging?.destination == LogDestination.Database) {
        opsQueryLog()
    }
}