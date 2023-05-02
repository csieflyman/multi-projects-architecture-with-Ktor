/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing

fun Application.staticContentRouting() {

    routing {
        staticResources("/public", "public")
    }
}