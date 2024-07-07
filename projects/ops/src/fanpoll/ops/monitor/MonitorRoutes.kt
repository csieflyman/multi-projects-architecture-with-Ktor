/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.monitor

import fanpoll.ops.OpsConst
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.monitorRoutes() {
    route("${OpsConst.urlRootPath}/monitor") {
        healthCheckRoute()
    }
}