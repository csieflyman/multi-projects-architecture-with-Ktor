/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.auth

import fanpoll.infra.auth.provider.*
import fanpoll.ops.OpsAuth

data class AuthConfig(
    private val root: ServiceAuthExternalConfig,
    private val monitor: ServiceAuthExternalConfig,
    private val user: UserAuthExternalConfig
) {

    val principalSourceAuthConfigs = listOf(
        PrincipalSourceAuthConfig(
            OpsAuth.RootSource,
            root.toServiceAuthConfig(OpsAuth.RootSource)
        ),
        PrincipalSourceAuthConfig(
            OpsAuth.MonitorSource,
            monitor.toServiceAuthConfig(OpsAuth.MonitorSource)
        ),
        PrincipalSourceAuthConfig(
            OpsAuth.UserSource,
            user.toServiceAuthConfig(OpsAuth.UserSource),
            user.toUserAuthConfig(OpsAuth.UserSource)
        ),
    )

    fun getServiceAuthConfigs(): List<ServiceAuthConfig> = principalSourceAuthConfigs.mapNotNull { it.service }

    fun getUserAuthConfigs(): List<UserSessionAuthConfig> = principalSourceAuthConfigs.mapNotNull { it.user }
}