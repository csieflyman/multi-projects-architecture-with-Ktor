/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.auth.authRoutes
import fanpoll.club.club.routes.clubRoutes
import fanpoll.club.notification.notificationRoutes
import fanpoll.club.user.routes.userRoutes
import fanpoll.infra.base.extension.listProjectAllRoutes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

private val logger = KotlinLogging.logger {}

fun Application.loadProjectRoutes() {
    routing {
        userRoutes()
        authRoutes()
        clubRoutes()
        notificationRoutes()

        logger.info { listProjectAllRoutes(ClubConst.projectId, ClubConst.urlRootPath) }
    }
}