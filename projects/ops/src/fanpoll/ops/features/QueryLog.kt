/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops.features

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.login.logging.LoginLogDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.util.queryDB
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.error.ErrorLogDTO
import fanpoll.infra.logging.request.RequestLogDTO
import fanpoll.infra.notification.logging.NotificationMessageLogDTO
import fanpoll.infra.openapi.dynamicQuery
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsOpenApi
import io.ktor.application.call
import io.ktor.routing.Routing
import io.ktor.routing.route
import org.koin.ktor.ext.inject

fun Routing.opsQueryLog() {

    val appConfig by inject<MyApplicationConfig>()

    route("${OpsConst.urlRootPath}/queryLog") {

        authorize(OpsAuth.OpsTeam) {

            if (appConfig.infra.logging!!.request.destination == LogDestination.Database) {
                route("/request") {
                    dynamicQuery<RequestLogDTO>(OpsOpenApi.QueryRequestLog) { dynamicQuery ->
                        call.respond(dynamicQuery.queryDB<RequestLogDTO>())
                    }
                }
            }

            if (appConfig.infra.logging!!.error.destination == LogDestination.Database) {
                route("/error") {
                    dynamicQuery<ErrorLogDTO>(OpsOpenApi.QueryErrorLog) { dynamicQuery ->
                        call.respond(dynamicQuery.queryDB<ErrorLogDTO>())
                    }
                }
            }

            if (appConfig.infra.sessionAuth!!.logging.destination == LogDestination.Database) {
                route("/login") {
                    dynamicQuery<LoginLogDTO>(OpsOpenApi.QueryLoginLog) { dynamicQuery ->
                        call.respond(dynamicQuery.queryDB<LoginLogDTO>())
                    }
                }
            }

            if (appConfig.infra.notification!!.logging.destination == LogDestination.Database) {
                route("/notificationMessage") {
                    dynamicQuery<NotificationMessageLogDTO>(OpsOpenApi.QueryNotificationMessageLog) { dynamicQuery ->
                        call.respond(dynamicQuery.queryDB<NotificationMessageLogDTO>())
                    }
                }
            }
        }
    }
}