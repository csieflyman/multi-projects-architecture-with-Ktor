/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login

import fanpoll.infra.app.UserDeviceTable
import fanpoll.infra.auth.login.logging.LoginLog
import fanpoll.infra.auth.login.session.MySessionStorage
import fanpoll.infra.auth.login.session.UserSession
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.sql.insert
import fanpoll.infra.database.sql.update
import fanpoll.infra.database.util.DBAsyncTaskCoroutineActor
import fanpoll.infra.logging.writers.LogWriter
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.time.Instant

class LoginService(
    private val sessionStorage: MySessionStorage,
    private val dbAsyncTaskCoroutineActor: DBAsyncTaskCoroutineActor,
    private val logWriter: LogWriter
) {

    private val logger = KotlinLogging.logger {}

    suspend fun loginSuccess(form: LoginForm<*>, sessionData: JsonObject? = null): UserPrincipal {
        val userPrincipal = UserPrincipal(
            form.userType, form.userId, form.userRoles, form.source,
            form.deviceId?.toString(), form.tenantId, false
        )
        val session = UserSession(userPrincipal, null, sessionData)
        userPrincipal.session = session

        logWriter.write(
            LoginLog(
                userPrincipal.userId, LoginResultCode.LOGIN_SUCCESS, userPrincipal.createAt,
                userPrincipal.source.projectId, userPrincipal.source, userPrincipal.tenantId,
                userPrincipal.clientId, form.clientVersion, form.ip,
                userPrincipal.sessionId()
            )
        )

        sessionStorage.setSession(session)

        if (form is AppLoginForm) {
            createOrUpdateUserDevice(form)
        }

        return userPrincipal
    }

    // maybe change to suspend in the future
    fun loginFail(form: LoginForm<*>, resultCode: LoginResultCode) {
        if (resultCode != LoginResultCode.ACCOUNT_NOT_FOUND) {
            logWriter.write(
                LoginLog(
                    form.userId, resultCode, Instant.now(),
                    form.source.projectId, form.source, form.tenantId,
                    form.deviceId?.toString(), form.clientVersion, form.ip
                )
            )
        }
        when (resultCode) {
            LoginResultCode.ACCOUNT_NOT_FOUND -> throw RequestException(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
            LoginResultCode.BAD_CREDENTIAL -> throw RequestException(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
            LoginResultCode.ACCOUNT_DISABLED -> throw RequestException(InfraResponseCode.AUTH_PRINCIPAL_DISABLED)
            LoginResultCode.TENANT_DISABLED -> throw RequestException(InfraResponseCode.AUTH_TENANT_DISABLED)
            LoginResultCode.OAUTH_NEW_USER -> throw InternalServerException(InfraResponseCode.NOT_IMPLEMENTED_ERROR)
            else -> error("undefined loginFail LoginResultCode: $resultCode")
        }
    }

    suspend fun logout(form: LogoutForm, userPrincipal: UserPrincipal) {
        logWriter.write(
            LoginLog(
                userPrincipal.userId, LoginResultCode.LOGOUT_SUCCESS, Instant.now(),
                userPrincipal.source.projectId, userPrincipal.source, userPrincipal.tenantId,
                userPrincipal.clientId, form.clientVersion, form.ip,
                userPrincipal.sessionId()
            )
        )

        sessionStorage.deleteSession(userPrincipal.session!!)
    }

    private fun createOrUpdateUserDevice(form: AppLoginForm) {
        if (form.userDevice == null) {
            dbAsyncTaskCoroutineActor.run("createUserDevice") {
                UserDeviceTable.insert(form.toCreateUserDeviceDTO())
            }
        } else {
            val updateDTO = form.toUpdateUserDeviceDTO()
            if (form.userDevice!!.osVersion != updateDTO.osVersion || form.userDevice!!.pushToken != updateDTO.pushToken) {
                dbAsyncTaskCoroutineActor.run("UpdateUserDevice") {
                    UserDeviceTable.update(updateDTO)
                }
            }
        }
    }
}