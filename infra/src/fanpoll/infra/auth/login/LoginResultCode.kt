/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode

enum class LoginResultCode {

    LOGIN_SUCCESS, LOGOUT_SUCCESS, ACCOUNT_NOT_FOUND, BAD_CREDENTIAL, ACCOUNT_DISABLED, OAUTH_NEW_USER;

    fun exception(): RuntimeException = when (this) {
        ACCOUNT_NOT_FOUND -> RequestException(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
        BAD_CREDENTIAL -> RequestException(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
        ACCOUNT_DISABLED -> RequestException(InfraResponseCode.AUTH_PRINCIPAL_DISABLED)
        OAUTH_NEW_USER -> InternalServerException(InfraResponseCode.NOT_IMPLEMENTED_ERROR)
        else -> error("undefined loginFail LoginResultCode: $this")
    }
}