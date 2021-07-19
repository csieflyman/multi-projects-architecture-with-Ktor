/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll

import io.ktor.application.Application
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing

fun Application.routing() {

    routing {
        static("public") {
            resources("public")
        }
    }
}