/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.release

import fanpoll.ops.OpsConst
import fanpoll.ops.release.app.routes.appReleaseRoute
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.releaseRoutes() {
    route("${OpsConst.urlRootPath}/release") {
        appReleaseRoute()
    }
}