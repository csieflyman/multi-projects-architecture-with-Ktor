/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club.user

import fanpoll.club.ClubAuth
import fanpoll.club.ClubConst
import fanpoll.club.ClubOpenApi
import fanpoll.club.ClubUserType
import fanpoll.infra.app.AppReleaseService
import fanpoll.infra.app.UserDeviceTable
import fanpoll.infra.auth.ClientVersionCheckResult
import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.login.*
import fanpoll.infra.auth.login.util.UserPasswordUtils
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.database.util.toSingleDTO
import fanpoll.infra.openapi.post
import fanpoll.infra.openapi.postEmptyBody
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.routing.Routing
import io.ktor.routing.route
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select
import org.koin.ktor.ext.inject

fun Routing.clubLogin() {

    val clubLoginService by inject<ClubLoginService>()
    val appReleaseService by inject<AppReleaseService>()

    route(ClubConst.urlRootPath) {

        authorize(ClubAuth.Public) {

            post<AppLoginForm, AppLoginResponse>("/login", ClubOpenApi.Login) { form ->
                form.populateRequest(call)

                val clientVersionCheckResult = if (form.checkClientVersion) appReleaseService.check(call) else null

                val loginResponse = if (clientVersionCheckResult == ClientVersionCheckResult.ForceUpdate) {
                    AppLoginResponse(null, clientVersionCheckResult)
                } else {
                    val userPrincipal = clubLoginService.login(form)
                    AppLoginResponse(userPrincipal.sessionId(), clientVersionCheckResult)
                }
                call.respond(DataResponseDTO(loginResponse))
            }
        }

        authorize(ClubAuth.User) {

            postEmptyBody("/logout", ClubOpenApi.Logout) {
                val form = LogoutForm()
                form.populateRequest(call)
                clubLoginService.logout(form, call.principal()!!)
                call.respond(CodeResponseDTO.OK)
            }
        }
    }

}

class ClubLoginService(private val loginService: LoginService) {

    fun login(form: AppLoginForm): UserPrincipal {
        val user = transaction {
            ClubUserTable.join(UserDeviceTable, JoinType.LEFT, ClubUserTable.id, UserDeviceTable.userId) {
                UserDeviceTable.sourceType eq form.appOs.principalType()
            }.slice(
                ClubUserTable.id, ClubUserTable.account, ClubUserTable.enabled, ClubUserTable.role, ClubUserTable.password,
                UserDeviceTable.id, UserDeviceTable.osVersion, UserDeviceTable.pushToken
            ).select { ClubUserTable.account eq form.account }.toList().toSingleDTO(UserDTO::class)
                ?: throw RequestException(ResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
        }

        val loginResultCode: LoginResultCode = if (!user.enabled!!)
            LoginResultCode.ACCOUNT_DISABLED
        else if (!UserPasswordUtils.verifyPassword(form.password, user.password!!))
            LoginResultCode.BAD_CREDENTIAL
        else LoginResultCode.LOGIN_SUCCESS

        form.populateUser(ClubUserType.User.value, user.id, setOf(user.role!!.value))
        form.userDevice = user.devices?.find { it.id == form.deviceId }

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