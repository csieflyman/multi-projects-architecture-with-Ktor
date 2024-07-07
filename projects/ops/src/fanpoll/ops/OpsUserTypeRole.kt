/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType

enum class OpsUserType : UserType {
    User;

    override val projectId: String = OpsConst.projectId

    override fun toString(): String = name
}

enum class OpsUserRole(override val type: UserType) : UserRole {
    OpsTeam(OpsUserType.User), AppTeam(OpsUserType.User);

    override fun toString(): String = name
}