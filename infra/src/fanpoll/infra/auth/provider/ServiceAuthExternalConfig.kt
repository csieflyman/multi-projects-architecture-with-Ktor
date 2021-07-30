/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.provider

import fanpoll.infra.auth.principal.PrincipalSource

data class ServiceAuthExternalConfig(
    val apiKey: String,
    val allowHosts: String? = null
) {
    fun toServiceAuthConfig(principalSource: PrincipalSource): ServiceAuthConfig =
        ServiceAuthConfig(principalSource, apiKey, allowHosts)
}