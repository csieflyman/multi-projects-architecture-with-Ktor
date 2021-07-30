/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.auth.provider

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.base.util.IdentifiableObject

class PrincipalSourceAuthConfig(
    val principalSource: PrincipalSource,
    val service: ServiceAuthConfig? = null,
    val user: UserSessionAuthConfig? = null,
    val runAs: UserRunAsAuthConfig? = null
) : IdentifiableObject<String>() {

    override val id: String
        get() = principalSource.id
}