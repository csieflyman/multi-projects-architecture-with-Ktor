/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login

enum class LoginResultCode {
    LOGIN_SUCCESS, LOGOUT_SUCCESS, ACCOUNT_NOT_FOUND, BAD_CREDENTIAL, ACCOUNT_DISABLED, TENANT_DISABLED, OAUTH_NEW_USER
}