/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.auth.login

import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.login.LoginResponse
import fanpoll.infra.auth.login.LogoutForm
import fanpoll.infra.auth.login.WebLoginForm
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.openapi.route.post
import fanpoll.infra.openapi.route.postEmptyBody
import fanpoll.infra.session.UserSession
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.auth.AuthOpenApi
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Route.loginRoute() {

    val loginService = OpsKoinContext.koin.get<LoginService>()

    authorize(OpsAuth.Public) {

        post<WebLoginForm, LoginResponse>("/login", AuthOpenApi.Login) { form ->
            form.populateRequest(call)
            val userSession = loginService.login(form)
            call.respond(DataResponseDTO(LoginResponse(userSession.sid)))
        }
    }

    authorize(OpsAuth.User) {

        postEmptyBody("/logout", AuthOpenApi.Logout) {
            val form = LogoutForm()
            form.populateRequest(call)
            loginService.logout(form, call.sessions.get<UserSession>()!!)
            call.respond(CodeResponseDTO.OK)
        }
    }
}