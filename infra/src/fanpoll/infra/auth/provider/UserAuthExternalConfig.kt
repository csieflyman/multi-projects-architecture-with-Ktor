/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.provider

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.session.SessionConfig

data class UserAuthExternalConfig(
    val apiKey: String,
    val runAsKey: String? = null,
    var session: SessionConfig? = null
) {

    fun toServiceAuthConfig(principalSource: PrincipalSource): ServiceAuthConfig =
        ServiceAuthConfig(principalSource, apiKey)

    fun toUserAuthConfig(principalSource: PrincipalSource): UserSessionAuthConfig =
        UserSessionAuthConfig(principalSource, apiKey, session)

    fun toRunAsConfig(principalSource: PrincipalSource): UserRunAsAuthConfig =
        UserRunAsAuthConfig(principalSource, runAsKey!!)
}