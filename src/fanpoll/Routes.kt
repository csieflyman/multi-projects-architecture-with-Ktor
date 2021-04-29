/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll

import fanpoll.club.club
import fanpoll.ops.operations
import io.ktor.application.Application
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing

fun Application.routing(appConfig: MyApplicationConfig) {

    routing {

        operations()

        club()

        static("public") {
            resources("public")
        }
    }
}