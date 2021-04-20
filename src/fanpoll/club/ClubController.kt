/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */
@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.club

import fanpoll.club.ClubOpenApiRoutes.CreateUser
import fanpoll.club.ClubOpenApiRoutes.DynamicReport
import fanpoll.club.ClubOpenApiRoutes.FindUsers
import fanpoll.club.ClubOpenApiRoutes.Login
import fanpoll.club.ClubOpenApiRoutes.Logout
import fanpoll.club.ClubOpenApiRoutes.PushNotification
import fanpoll.club.ClubOpenApiRoutes.UpdateMyPassword
import fanpoll.club.ClubOpenApiRoutes.UpdateUser
import fanpoll.club.features.*
import fanpoll.infra.DataResponse
import fanpoll.infra.HttpStatusResponse
import fanpoll.infra.app.AppReleaseService
import fanpoll.infra.auth.*
import fanpoll.infra.controller.UUIDEntityIdLocation
import fanpoll.infra.database.dynamicDBQuery
import fanpoll.infra.login.AppLoginForm
import fanpoll.infra.login.LoginResponse
import fanpoll.infra.notification.NotificationSender
import fanpoll.infra.openapi.support.*
import fanpoll.infra.respondMyResponse
import fanpoll.ops.OpsAuth
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import java.util.*

@OptIn(KtorExperimentalLocationsAPI::class)
fun Routing.club() {

    route(ClubConst.urlRootPath) {

        authorize(OpsAuth.OperationsTeam) {

            route("/users") {

                post<CreateUserForm, Long>(CreateUser) { dto ->
                    val id = UserService.createUser(dto)
                    call.respond(DataResponse.uuid(id))
                }

                putLocation<UUIDEntityIdLocation, UpdateUserForm, Unit>(UpdateUser) { _, dto ->
                    UserService.updateUser(dto)
                    call.respond(HttpStatusCode.OK)
                }

                dynamicQuery<UserDTO>(FindUsers) {
                    call.respondMyResponse(call.request.dynamicDBQuery<UserDTO>())
                }
            }

            post<ClubSendPushNotificationForm, UUID>("/notification/push", PushNotification) { dto ->
                val message = ClubNotificationTypes.BroadCast.buildChannelMessage(dto)
                //val message = NotificationCmdMessage.create(ClubNotificationTypes.BroadCast, dto)
                NotificationSender.sendAsync(message)
                call.respond(DataResponse.uuid(message.id))
            }

            post<ClubDynamicReportForm, Unit>("/data/export", DynamicReport) { dto ->
                val message = ClubNotificationTypes.DynamicReport.buildChannelMessage(dto)
                //val message = NotificationCmdMessage.create(ClubNotificationTypes.DynamicReport, dto)
                NotificationSender.sendAsync(message)
                call.respond(DataResponse.uuid(message.id))
            }
        }

        authorize(ClubAuth.Public) {

            post<AppLoginForm, LoginResponse>("/login", Login) { dto ->
                dto.populateRequest(call)

                val clientVersionCheckResult = call.attributes.getOrNull(ATTRIBUTE_KEY_CLIENT_VERSION_RESULT)
                    ?: if (dto.checkClientVersion) AppReleaseService.check(call.principal<ServicePrincipal>()!!, call) else null

                val loginResponse = if (clientVersionCheckResult == ClientVersionCheckResult.ForceUpdate) {
                    LoginResponse(null, clientVersionCheckResult)
                } else {
                    val sid = UserService.login(dto)
                    LoginResponse(sid, clientVersionCheckResult)
                }
                call.respond(DataResponse(loginResponse))
            }
        }

        authorize(ClubAuth.User) {

            postEmptyBody("/logout", Logout) {
                val userPrincipal = call.principal<UserPrincipal>()!!
                UserService.logout(userPrincipal)
                call.respond(HttpStatusResponse.OK)
            }

            put<UpdateUserPasswordForm, Unit>("/myPassword", UpdateMyPassword) { dto ->
                val userId = call.principal<UserPrincipal>()!!.userId
                UserService.updatePassword(userId, dto)
                call.respond(HttpStatusResponse.OK)
            }
        }
    }
}