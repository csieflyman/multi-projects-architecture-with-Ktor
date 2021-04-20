/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.login

import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.UserPrincipal
import fanpoll.infra.logging.LogManager
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.LogType
import fanpoll.infra.logging.LoginLogDTO
import fanpoll.infra.model.TenantId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.time.Instant

object LoginService {

    private val logger = KotlinLogging.logger {}

    fun webLogin(formDTO: WebLoginForm, resultCode: LoginResultCode, sessionData: JsonObject? = null): LoginResult {
        return login(formDTO, resultCode, sessionData)
    }

    fun appLogin(formDTO: AppLoginForm, resultCode: LoginResultCode, sessionData: JsonObject? = null): LoginResult {
        return login(formDTO, resultCode, sessionData)
    }

    private fun login(form: LoginForm<*>, resultCode: LoginResultCode, sessionData: JsonObject? = null): LoginResult {
        var userPrincipal: UserPrincipal? = null
        if (resultCode != LoginResultCode.ACCOUNT_NOT_FOUND) {
            runBlocking {
                if (resultCode == LoginResultCode.SUCCESS) {
                    userPrincipal = UserPrincipal(
                        form.userType, form.userId,
                        form.userRoles, form.source,
                        form.deviceId?.toString(),
                        form.tenantId?.let { TenantId(it) },
                        false
                    )
                    userPrincipal!!.session = UserSession(userPrincipal!!, Instant.now(), null, sessionData)
                    SessionService.login(userPrincipal!!.session!!)
                }

                LogManager.write(
                    LogMessage(
                        LogType.LOGIN, LoginLogDTO(
                            Instant.now(), resultCode,
                            form.userId,
                            form.source.projectId, form.source, form.tenantId,
                            form.deviceId?.toString(), form.clientVersion, form.ip,
                            userPrincipal?.sessionId()
                        )
                    )
                )
            }
        }

        return when (resultCode) {
            LoginResultCode.SUCCESS -> LoginResult(resultCode, userPrincipal)
            LoginResultCode.ACCOUNT_NOT_FOUND -> throw RequestException(ResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
            LoginResultCode.BAD_CREDENTIAL -> throw RequestException(ResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
            LoginResultCode.ACCOUNT_DISABLED -> throw RequestException(ResponseCode.AUTH_PRINCIPAL_DISABLED)
            LoginResultCode.TENANT_DISABLED -> throw RequestException(ResponseCode.AUTH_TENANT_DISABLED)
            LoginResultCode.OAUTH_NEW_USER -> throw InternalServerErrorException(ResponseCode.NOT_IMPLEMENTED_ERROR)
        }
    }

    fun logout(principal: UserPrincipal) {
        runBlocking {
            SessionService.logout(principal.session!!)
        }
    }
}