/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.base.extension.listProjectAllRoutes
import fanpoll.ops.auth.authRoutes
import fanpoll.ops.log.logRoutes
import fanpoll.ops.monitor.monitorRoutes
import fanpoll.ops.notification.notificationRoutes
import fanpoll.ops.release.releaseRoutes
import fanpoll.ops.report.reportRoutes
import fanpoll.ops.user.routes.userRoutes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

private val logger = KotlinLogging.logger {}

fun Application.loadProjectRoutes() {
    routing {
        userRoutes()
        authRoutes()
        notificationRoutes()
        reportRoutes()
        releaseRoutes()
        monitorRoutes()
        logRoutes()

        logger.info { listProjectAllRoutes(OpsConst.projectId, OpsConst.urlRootPath) }
    }
}