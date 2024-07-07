/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.report

import fanpoll.ops.OpsConst
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.reportRoutes() {
    route("${OpsConst.urlRootPath}/reports") {
        exportReport()
    }
}