/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.auth.principal.ClientSource
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserRole

object OpsAuth {

    const val serviceAuthProviderName = "${OpsConst.projectId}-service"
    const val userAuthProviderName = "${OpsConst.projectId}-user"

    val RootSource = PrincipalSource(OpsConst.projectId, "root", ClientSource.Service, false)
    val MonitorSource = PrincipalSource(OpsConst.projectId, "monitor", ClientSource.Service, false)
    val UserSource = PrincipalSource(OpsConst.projectId, "user", ClientSource.Browser, false)

    val Root = PrincipalAuth.Service(serviceAuthProviderName, OpsProjectOpenApi.AllSecuritySchemes, setOf(RootSource))
    val Monitor = PrincipalAuth.Service(serviceAuthProviderName, OpsProjectOpenApi.AllSecuritySchemes, setOf(MonitorSource))
    val Public = PrincipalAuth.Service(serviceAuthProviderName, OpsProjectOpenApi.AllSecuritySchemes, setOf(UserSource))

    val User = PrincipalAuth.User(
        userAuthProviderName, OpsProjectOpenApi.AllSecuritySchemes, setOf(UserSource),
        mapOf(OpsUserType.User to UserRole.getRolesByType(OpsUserType.User))
    )

    val OpsTeam = PrincipalAuth.User(
        userAuthProviderName, OpsProjectOpenApi.AllSecuritySchemes, setOf(UserSource),
        mapOf(OpsUserType.User to setOf(OpsUserRole.OpsTeam))
    )

    val AppTeam = PrincipalAuth.User(
        userAuthProviderName, OpsProjectOpenApi.AllSecuritySchemes, setOf(UserSource),
        mapOf(OpsUserType.User to setOf(OpsUserRole.OpsTeam, OpsUserRole.AppTeam))
    )
}