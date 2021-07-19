/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import fanpoll.infra.auth.login.session.UserSession
import fanpoll.infra.base.tenant.TenantId
import java.time.Instant
import java.util.*

class UserPrincipal(
    val userType: UserType,
    val userId: UUID,
    val roles: Set<UserRole>? = null,
    override val source: PrincipalSource,
    val clientId: String? = null,
    val tenantId: TenantId? = null,
    val runAs: Boolean = false,
    var session: UserSession? = null
) : MyPrincipal() {

    override val id = "${userType.id}-$userId"

    val createAt: Instant = Instant.now()

    override fun toString(): String {
        return "${if (runAs) "(runAs)" else ""}${userType.id}-$userId-$roles-$source-${clientId ?: "?"}${if (tenantId != null) "-$tenantId" else ""}"
    }

    fun sessionId(): String = session!!.id.value
}