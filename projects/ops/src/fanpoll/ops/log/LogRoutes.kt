/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.log

import fanpoll.ops.OpsConst
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.logRoutes() {
    route("${OpsConst.urlRootPath}/log") {
        queryLogRoute()
    }
}