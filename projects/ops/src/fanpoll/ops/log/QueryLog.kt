/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.log

import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.login.logging.LoginLogDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.config.MyApplicationConfig
import fanpoll.infra.database.exposed.util.queryDB
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.error.ErrorLogDTO
import fanpoll.infra.logging.request.RequestLogDTO
import fanpoll.infra.notification.logging.NotificationMessageLogDTO
import fanpoll.infra.openapi.route.dynamicQuery
import fanpoll.ops.OpsAuth
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.queryLogRoute() {

    val appConfig by inject<MyApplicationConfig>()

    authorize(OpsAuth.OpsTeam) {

        if (appConfig.infra.logging.request.destination == LogDestination.Database) {
            route("/request") {
                dynamicQuery<RequestLogDTO>(LogOpenApi.QueryRequestLog) { dynamicQuery ->
                    call.respond(dynamicQuery.queryDB<RequestLogDTO>())
                }
            }
        }

        if (appConfig.infra.logging.error.destination == LogDestination.Database) {
            route("/error") {
                dynamicQuery<ErrorLogDTO>(LogOpenApi.QueryErrorLog) { dynamicQuery ->
                    call.respond(dynamicQuery.queryDB<ErrorLogDTO>())
                }
            }
        }

        if (appConfig.infra.sessionAuth.logging.destination == LogDestination.Database) {
            route("/login") {
                dynamicQuery<LoginLogDTO>(LogOpenApi.QueryLoginLog) { dynamicQuery ->
                    call.respond(dynamicQuery.queryDB<LoginLogDTO>())
                }
            }
        }

        if (appConfig.infra.notification.logging.destination == LogDestination.Database) {
            route("/notificationMessage") {
                dynamicQuery<NotificationMessageLogDTO>(LogOpenApi.QueryNotificationMessageLog) { dynamicQuery ->
                    call.respond(dynamicQuery.queryDB<NotificationMessageLogDTO>())
                }
            }
        }
    }
}