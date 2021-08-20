/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.PrincipalSourceType
import fanpoll.infra.auth.provider.*
import fanpoll.infra.openapi.ProjectOpenApi
import fanpoll.ops.OpsAuth.MonitorSource
import fanpoll.ops.OpsAuth.RootSource
import fanpoll.ops.OpsAuth.UserSource

object OpsAuth {

    const val serviceAuthProviderName = "${OpsConst.projectId}-service"
    const val userAuthProviderName = "${OpsConst.projectId}-user"

    val allAuthSchemes = listOf(ProjectOpenApi.apiKeySecurityScheme)

    val RootSource = PrincipalSource(OpsConst.projectId, "root", PrincipalSourceType.Postman, false)
    val MonitorSource = PrincipalSource(OpsConst.projectId, "monitor", PrincipalSourceType.Ops, false)
    val UserSource = PrincipalSource(OpsConst.projectId, "user", PrincipalSourceType.Postman, true)

    val Root = PrincipalAuth.Service(serviceAuthProviderName, allAuthSchemes, setOf(RootSource))
    val Monitor = PrincipalAuth.Service(serviceAuthProviderName, allAuthSchemes, setOf(MonitorSource))
    val Public = PrincipalAuth.Service(serviceAuthProviderName, allAuthSchemes, setOf(UserSource))

    val User = PrincipalAuth.User(
        userAuthProviderName, allAuthSchemes, setOf(UserSource),
        mapOf(OpsUserType.User.value to OpsUserType.User.value.roles)
    )

    val OpsTeam = PrincipalAuth.User(
        userAuthProviderName, allAuthSchemes, setOf(UserSource),
        mapOf(OpsUserType.User.value to setOf(OpsUserRole.OpsTeam.value))
    )

    val AppTeam = PrincipalAuth.User(
        userAuthProviderName, allAuthSchemes, setOf(UserSource),
        mapOf(OpsUserType.User.value to setOf(OpsUserRole.OpsTeam.value, OpsUserRole.AppTeam.value))
    )
}

data class OpsAuthConfig(
    private val root: ServiceAuthExternalConfig,
    private val monitor: ServiceAuthExternalConfig,
    private val user: UserAuthExternalConfig
) {

    val principalSourceAuthConfigs = listOf(
        PrincipalSourceAuthConfig(
            RootSource,
            root.toServiceAuthConfig(RootSource)
        ),
        PrincipalSourceAuthConfig(
            MonitorSource,
            monitor.toServiceAuthConfig(MonitorSource)
        ),
        PrincipalSourceAuthConfig(
            UserSource,
            user.toServiceAuthConfig(UserSource),
            user.toUserAuthConfig(UserSource)
        ),
    )

    fun getServiceAuthConfigs(): List<ServiceAuthConfig> = principalSourceAuthConfigs.mapNotNull { it.service }

    fun getUserAuthConfigs(): List<UserSessionAuthConfig> = principalSourceAuthConfigs.mapNotNull { it.user }
}