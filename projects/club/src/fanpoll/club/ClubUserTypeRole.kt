/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType

enum class ClubUserType : UserType {
    User;

    override val projectId: String = ClubConst.projectId

    override fun toString(): String = name
}

enum class ClubUserRole(override val type: UserType) : UserRole {
    Admin(ClubUserType.User), User(ClubUserType.User);

    override fun toString(): String = name
}
