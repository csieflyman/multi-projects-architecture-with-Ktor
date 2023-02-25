/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import io.ktor.server.application.Application
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.routing.routing

fun Application.staticContentRouting() {

    routing {
        static("public") {
            resources("public")
        }
    }
}