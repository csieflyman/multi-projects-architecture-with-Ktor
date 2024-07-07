/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.auth.principal.ClientSource
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserRole

object ClubAuth {

    const val serviceAuthProviderName = "${ClubConst.projectId}-service"
    const val userAuthProviderName = "${ClubConst.projectId}-user"
    const val userRunAsAuthProviderName = "${ClubConst.projectId}-runAs"

    val RootSource = PrincipalSource(ClubConst.projectId, "root", ClientSource.Service, false)
    val Root = PrincipalAuth.Service(serviceAuthProviderName, ClubProjectOpenApi.AllSecuritySchemes, setOf(RootSource))

    val Android: PrincipalSource = PrincipalSource(
        ClubConst.projectId, "android", ClientSource.Android, true
    )
    val iOS: PrincipalSource = PrincipalSource(
        ClubConst.projectId, "iOS", ClientSource.iOS, true
    )
    private val App: Set<PrincipalSource> = setOf(Android, iOS)

    val Public = PrincipalAuth.Service(serviceAuthProviderName, ClubProjectOpenApi.AllSecuritySchemes, App)

    val User = PrincipalAuth.User(
        userAuthProviderName, ClubProjectOpenApi.AllSecuritySchemes, App,
        mapOf(ClubUserType.User to UserRole.getRolesByType(ClubUserType.User)),
        runAsAuthProviderName = userRunAsAuthProviderName
    )
    val Admin = PrincipalAuth.User(
        userAuthProviderName, ClubProjectOpenApi.AllSecuritySchemes, App,
        mapOf(ClubUserType.User to setOf(ClubUserRole.Admin)),
        runAsAuthProviderName = userRunAsAuthProviderName
    )
}