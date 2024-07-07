/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.auth.login

import fanpoll.infra.auth.login.LoginResultCode
import fanpoll.infra.auth.login.LogoutForm
import fanpoll.infra.auth.login.WebLoginForm
import fanpoll.infra.auth.login.logging.LoginLog
import fanpoll.infra.auth.login.util.UserPasswordUtils
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.session.MySessionStorage
import fanpoll.infra.session.UserSession
import fanpoll.ops.OpsUserType
import fanpoll.ops.user.domain.User
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.runBlocking
import java.time.Instant

class LoginService(
    private val userLoginRepository: UserLoginRepository,
    private val sessionStorage: MySessionStorage,
    private val logWriter: LogWriter
) {

    @WithSpan
    suspend fun login(form: WebLoginForm): UserSession {
        val user = userLoginRepository.findUserByAccount(form.account)
            ?: throw RequestException(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED)

        val loginResultCode: LoginResultCode = if (!user.enabled!!)
            LoginResultCode.ACCOUNT_DISABLED
        else if (!UserPasswordUtils.verifyPassword(form.password, user.password!!))
            LoginResultCode.BAD_CREDENTIAL
        else LoginResultCode.LOGIN_SUCCESS

        val userSession = createUserSession(form, user)

        logWriter.write(
            LoginLog(
                user.id, loginResultCode,
                form.source.projectId, form.source.name,
                null, form.clientVersion, form.ip, form.traceId,
                userSession.sid
            )
        )

        if (loginResultCode != LoginResultCode.LOGIN_SUCCESS) {
            throw loginResultCode.exception()
        }
        return userSession
    }

    suspend fun logout(form: LogoutForm, userSession: UserSession) {
        logWriter.write(
            LoginLog(
                userSession.userId, LoginResultCode.LOGOUT_SUCCESS,
                userSession.source.projectId, userSession.source.name,
                userSession.clientId, userSession.clientVersion, form.ip, form.traceId,
                userSession.sid
            )
        )

        runBlocking {
            sessionStorage.deleteSession(userSession)
        }
    }

    private suspend fun createUserSession(form: WebLoginForm, user: User): UserSession {
        val sessionConfig = form.source.getAuthConfig().user!!.session
        val loginTime = Instant.now()
        val expireTime = sessionConfig?.expireDuration?.let { loginTime.plus(it) }
        val userSession = UserSession(
            user.account!!, form.source,
            user.id, OpsUserType.User, user.roles, false,
            null, form.clientVersion, loginTime, expireTime
        )
        sessionStorage.setSession(userSession)
        return userSession
    }
}