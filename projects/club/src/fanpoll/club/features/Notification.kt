/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club.features

import fanpoll.club.ClubAuth
import fanpoll.club.ClubConst
import fanpoll.club.ClubNotification
import fanpoll.club.ClubOpenApi
import fanpoll.infra.auth.authorize
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.notification.senders.NotificationSender
import fanpoll.infra.notification.util.SendNotificationForm
import fanpoll.infra.openapi.post
import io.ktor.application.call
import io.ktor.routing.Routing
import io.ktor.routing.route
import org.koin.ktor.ext.inject
import java.util.*

fun Routing.clubNotification() {

    val notificationSender by inject<NotificationSender>()

    route("${ClubConst.urlRootPath}/notification") {

        authorize(ClubAuth.Admin) {

            post<SendNotificationForm, UUID>("/send", ClubOpenApi.SendNotification) { form ->
                val notification = form.toNotification(ClubNotification.SendNotification)
                notificationSender.send(notification)
                call.respond(DataResponseDTO.uuid(notification.id))
            }
        }
    }

}