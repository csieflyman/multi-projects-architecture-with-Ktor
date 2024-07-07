/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.auth

import fanpoll.club.ClubConst
import fanpoll.club.auth.login.loginRoutes
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.authRoutes() {
    route("${ClubConst.urlRootPath}/auth") {
        loginRoutes()
    }
}