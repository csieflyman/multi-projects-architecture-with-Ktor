/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops.features

import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.respond
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.senders.NotificationSender
import fanpoll.infra.openapi.post
import fanpoll.ops.*
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.routing.Routing
import io.ktor.routing.route
import org.koin.ktor.ext.inject
import java.util.*

fun Routing.opsDataReport() {

    val notificationSender by inject<NotificationSender>()
    val opsUserService by inject<OpsUserService>()

    route("${OpsConst.urlRootPath}/data/report") {

        authorize(OpsAuth.OpsTeam) {

            post<DataReportForm, UUID>(OpsOpenApi.DataReport) { form ->
                if (form.email == null)
                    form.email = opsUserService.getUserById(call.principal<UserPrincipal>()!!.userId).email
                if (form.email == null)
                    throw RequestException(InfraResponseCode.BAD_REQUEST_BODY, "email is missing")

                val notification = Notification(
                    OpsNotification.DataReport, lazyLoadArg = form
                )
                notificationSender.send(notification)
                call.respond(DataResponseDTO.uuid(notification.id))
            }
        }
    }

}