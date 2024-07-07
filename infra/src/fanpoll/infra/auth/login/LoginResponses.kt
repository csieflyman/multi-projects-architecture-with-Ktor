/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(val sid: String)

@Serializable
data class AppLoginResponse(val sid: String? = null, var clientVersionCheckResult: ClientVersionCheckResult? = null)