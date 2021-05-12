/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */
@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.club

import fanpoll.club.ClubOpenApiOperations.CreateUser
import fanpoll.club.ClubOpenApiOperations.DynamicReport
import fanpoll.club.ClubOpenApiOperations.FindUsers
import fanpoll.club.ClubOpenApiOperations.Login
import fanpoll.club.ClubOpenApiOperations.Logout
import fanpoll.club.ClubOpenApiOperations.PushNotification
import fanpoll.club.ClubOpenApiOperations.UpdateMyPassword
import fanpoll.club.ClubOpenApiOperations.UpdateUser
import fanpoll.club.features.*
import fanpoll.infra.CodeResponseDTO
import fanpoll.infra.DataResponseDTO
import fanpoll.infra.app.AppReleaseService
import fanpoll.infra.auth.*
import fanpoll.infra.controller.UUIDEntityIdLocation
import fanpoll.infra.database.queryDB
import fanpoll.infra.login.AppLoginForm
import fanpoll.infra.login.LoginResponse
import fanpoll.infra.notification.NotificationSender
import fanpoll.infra.openapi.dynamicQuery
import fanpoll.infra.openapi.post
import fanpoll.infra.openapi.postEmptyBody
import fanpoll.infra.openapi.put
import fanpoll.infra.respond
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
                call.respond(DataResponseDTO(loginResponse))
            }
        }

        authorize(ClubAuth.User) {

            postEmptyBody("/logout", Logout) {
                val userPrincipal = call.principal<UserPrincipal>()!!
                UserService.logout(userPrincipal)
                call.respond(CodeResponseDTO.OK)
            }

            put<UpdateUserPasswordForm, Unit>("/myPassword", UpdateMyPassword) { dto ->
                val userId = call.principal<UserPrincipal>()!!.userId
                UserService.updatePassword(userId, dto)
                call.respond(CodeResponseDTO.OK)
            }
        }

        authorize(OpsAuth.OpsTeam, ClubAuth.Admin) {

            route("/users") {

                post<CreateUserForm, Long>(CreateUser) { dto ->
                    val id = UserService.createUser(dto)
                    call.respond(DataResponseDTO.uuid(id))
                }

                put<UUIDEntityIdLocation, UpdateUserForm, Unit>(UpdateUser) { _, dto ->
                    UserService.updateUser(dto)
                    call.respond(HttpStatusCode.OK)
                }

                dynamicQuery<UserDTO>(FindUsers) { dynamicQuery ->
                    call.respond(dynamicQuery.queryDB<UserDTO>())
                }
            }
        }

        authorize(OpsAuth.OpsTeam) {

            post<ClubSendPushNotificationForm, UUID>("/notification/push", PushNotification) { dto ->
                val message = ClubNotificationTypes.BroadCast.buildChannelMessage(dto)
                //val message = NotificationCmdMessage.create(ClubNotificationTypes.BroadCast, dto)
                NotificationSender.sendAsync(message)
                call.respond(DataResponseDTO.uuid(message.id))
            }

            post<ClubDynamicReportForm, UUID>("/data/export", DynamicReport) { dto ->
                val message = ClubNotificationTypes.DynamicReport.buildChannelMessage(dto)
                //val message = NotificationCmdMessage.create(ClubNotificationTypes.DynamicReport, dto)
                NotificationSender.sendAsync(message)
                call.respond(DataResponseDTO.uuid(message.id))
            }
        }
    }
}