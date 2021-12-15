/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops.features

import fanpoll.infra.auth.authorize
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.util.queryDB
import fanpoll.infra.notification.logging.NotificationMessageLogDTO
import fanpoll.infra.openapi.dynamicQuery
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsOpenApi
import io.ktor.application.call
import io.ktor.routing.Routing
import io.ktor.routing.route

fun Routing.opsQueryLog() {

    route("${OpsConst.urlRootPath}/queryLog") {

        authorize(OpsAuth.OpsTeam) {

            route("/notificationMessage") {
                dynamicQuery<NotificationMessageLogDTO>(OpsOpenApi.QueryNotificationMessageLog) { dynamicQuery ->
                    call.respond(dynamicQuery.queryDB<NotificationMessageLogDTO>())
                }
            }
        }
    }
}