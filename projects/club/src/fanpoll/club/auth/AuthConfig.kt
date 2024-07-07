/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.auth

import fanpoll.club.ClubAuth
import fanpoll.infra.auth.provider.*

data class AuthConfig(
    private val root: ServiceAuthExternalConfig,
    private val android: UserAuthExternalConfig,
    private val iOS: UserAuthExternalConfig
) {

    val principalSourceAuthConfigs = listOf(
        PrincipalSourceAuthConfig(
            ClubAuth.RootSource,
            root.toServiceAuthConfig(ClubAuth.RootSource)
        ),
        PrincipalSourceAuthConfig(
            ClubAuth.Android,
            android.toServiceAuthConfig(ClubAuth.Android),
            android.toUserAuthConfig(ClubAuth.Android),
            android.runAsKey?.let { android.toRunAsConfig(ClubAuth.Android) }
        ),
        PrincipalSourceAuthConfig(
            ClubAuth.iOS,
            iOS.toServiceAuthConfig(ClubAuth.iOS),
            iOS.toUserAuthConfig(ClubAuth.iOS),
            iOS.runAsKey?.let { iOS.toRunAsConfig(ClubAuth.iOS) }
        )
    )

    fun getServiceAuthConfigs(): List<ServiceAuthConfig> = principalSourceAuthConfigs.mapNotNull { it.service }

    fun getUserAuthConfigs(): List<UserSessionAuthConfig> = principalSourceAuthConfigs.mapNotNull { it.user }

    fun getRunAsConfigs(): List<UserRunAsAuthConfig> = principalSourceAuthConfigs.mapNotNull { it.runAs }
}