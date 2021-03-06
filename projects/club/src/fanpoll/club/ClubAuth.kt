/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.ClubAuth.RootSource
import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.PrincipalSourceType
import fanpoll.infra.auth.provider.*
import fanpoll.infra.openapi.ProjectOpenApi

object ClubAuth {

    const val serviceAuthProviderName = "${ClubConst.projectId}-service"
    const val userAuthProviderName = "${ClubConst.projectId}-user"
    const val userRunAsAuthProviderName = "${ClubConst.projectId}-runAs"

    val allAuthSchemes = listOf(ProjectOpenApi.apiKeySecurityScheme)

    val RootSource = PrincipalSource(ClubConst.projectId, "root", PrincipalSourceType.Postman, false)
    val Root = PrincipalAuth.Service(serviceAuthProviderName, allAuthSchemes, setOf(RootSource))

    val Android: PrincipalSource = PrincipalSource(
        ClubConst.projectId, "android", PrincipalSourceType.Android, true
    )
    val iOS: PrincipalSource = PrincipalSource(
        ClubConst.projectId, "iOS", PrincipalSourceType.iOS, true
    )
    private val App: Set<PrincipalSource> = setOf(Android, iOS)

    val Public = PrincipalAuth.Service(serviceAuthProviderName, allAuthSchemes, App)

    val User = PrincipalAuth.User(
        userAuthProviderName, allAuthSchemes, App,
        mapOf(ClubUserType.User.value to ClubUserType.User.value.roles),
        runAsAuthProviderName = userRunAsAuthProviderName
    )
    val Admin = PrincipalAuth.User(
        userAuthProviderName, allAuthSchemes, App,
        mapOf(ClubUserType.User.value to setOf(ClubUserRole.Admin.value)),
        runAsAuthProviderName = userRunAsAuthProviderName
    )
    val Member = PrincipalAuth.User(
        userAuthProviderName, allAuthSchemes, App,
        mapOf(ClubUserType.User.value to setOf(ClubUserRole.Member.value)),
        runAsAuthProviderName = userRunAsAuthProviderName
    )
}

data class ClubAuthConfig(
    private val root: ServiceAuthExternalConfig,
    private val android: UserAuthExternalConfig,
    private val iOS: UserAuthExternalConfig
) {

    val principalSourceAuthConfigs = listOf(
        PrincipalSourceAuthConfig(
            RootSource,
            root.toServiceAuthConfig(RootSource)
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