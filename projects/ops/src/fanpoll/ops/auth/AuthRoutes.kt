/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.auth

import fanpoll.ops.OpsConst
import fanpoll.ops.auth.login.loginRoute
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.authRoutes() {
    route("${OpsConst.urlRootPath}/auth") {
        loginRoute()
    }
}