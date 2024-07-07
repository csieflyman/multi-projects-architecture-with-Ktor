/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.auth.login

import fanpoll.club.ClubAuth
import fanpoll.club.ClubKoinContext
import fanpoll.club.auth.AuthOpenApi
import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.login.AppLoginResponse
import fanpoll.infra.auth.login.ClientVersionCheckResult
import fanpoll.infra.auth.login.LogoutForm
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.openapi.route.post
import fanpoll.infra.openapi.route.postEmptyBody
import fanpoll.infra.release.app.service.AppReleaseChecker
import fanpoll.infra.session.UserSession
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.koin.ktor.ext.inject

fun Route.loginRoutes() {

    val clubLoginService = ClubKoinContext.koin.get<ClubLoginService>()
    val appReleaseChecker by inject<AppReleaseChecker>()

    authorize(ClubAuth.Public) {

        post<AppLoginForm, AppLoginResponse>("/login", AuthOpenApi.Login) { form ->
            form.populateRequest(call)

            val clientVersionCheckResult = if (form.checkClientVersion) appReleaseChecker.check(call) else null

            val loginResponse = if (clientVersionCheckResult == ClientVersionCheckResult.ForceUpdate) {
                AppLoginResponse(null, clientVersionCheckResult)
            } else {
                val userSession = clubLoginService.login(form)
                AppLoginResponse(userSession.sid, clientVersionCheckResult)
            }
            call.respond(DataResponseDTO(loginResponse))
        }
    }

    authorize(ClubAuth.User) {

        postEmptyBody("/logout", AuthOpenApi.Logout) {
            val form = LogoutForm()
            form.populateRequest(call)
            clubLoginService.logout(form, call.sessions.get<UserSession>()!!)
            call.respond(CodeResponseDTO.OK)
        }
    }
}