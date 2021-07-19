/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType

enum class OpsUserType(val value: UserType) {

    User(object : UserType(OpsConst.projectId, "user") {

        override val roles: Set<UserRole> = setOf(UserRole(id, "opsTeam"), UserRole(id, "appTeam"))
    })
}

enum class OpsUserRole(val value: UserRole) {

    OpsTeam(OpsUserType.User.value.roles!!.first { it.name == "opsTeam" }),
    AppTeam(OpsUserType.User.value.roles!!.first { it.name == "appTeam" })
}