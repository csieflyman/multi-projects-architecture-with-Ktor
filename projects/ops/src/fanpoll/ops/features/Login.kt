/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops.features

import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.login.*
import fanpoll.infra.auth.login.util.UserPasswordUtils
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.database.util.toSingleDTO
import fanpoll.infra.openapi.post
import fanpoll.infra.openapi.postEmptyBody
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsOpenApi
import fanpoll.ops.OpsUserType
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.routing.Routing
import io.ktor.routing.route
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.select
import org.koin.ktor.ext.inject

fun Routing.opsLogin() {

    val opsLoginService by inject<OpsLoginService>()

    route(OpsConst.urlRootPath) {

        authorize(OpsAuth.Public) {

            post<WebLoginForm, LoginResponse>("/login", OpsOpenApi.Login) { form ->
                form.populateRequest(call)
                val userPrincipal = opsLoginService.login(form)
                call.respond(DataResponseDTO(LoginResponse(userPrincipal.sessionId())))
            }
        }

        authorize(OpsAuth.User) {

            postEmptyBody("/logout", OpsOpenApi.Logout) {
                val form = LogoutForm()
                form.populateRequest(call)
                opsLoginService.logout(form, call.principal()!!)
                call.respond(CodeResponseDTO.OK)
            }
        }
    }
}

class OpsLoginService(private val loginService: LoginService) {

    fun login(form: WebLoginForm): UserPrincipal {
        val user = transaction {
            OpsUserTable
                .slice(OpsUserTable.id, OpsUserTable.account, OpsUserTable.enabled, OpsUserTable.role, OpsUserTable.password)
                .select { OpsUserTable.account eq form.account }.toList().toSingleDTO(UserDTO::class)
                ?: throw RequestException(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
        }

        val loginResultCode: LoginResultCode = if (!user.enabled!!)
            LoginResultCode.ACCOUNT_DISABLED
        else if (!UserPasswordUtils.verifyPassword(form.password, user.password!!))
            LoginResultCode.BAD_CREDENTIAL
        else LoginResultCode.LOGIN_SUCCESS

        form.populateUser(OpsUserType.User.value, user.id, setOf(user.role!!.value))

        return if (loginResultCode == LoginResultCode.LOGIN_SUCCESS) {
            runBlocking {
                loginService.loginSuccess(form)
            }
        } else {
            loginService.loginFail(form, loginResultCode) // loginFail throw exception
            error("unreachable code")
        }
    }

    fun logout(form: LogoutForm, principal: UserPrincipal) {
        runBlocking {
            loginService.logout(form, principal)
        }
    }
}