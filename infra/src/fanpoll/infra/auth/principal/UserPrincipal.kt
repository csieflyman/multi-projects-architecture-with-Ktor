/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import java.util.*

interface UserPrincipal : MyPrincipal {
    val userId: UUID
    val account: String
    val userType: UserType
    val userRoles: Set<UserRole>
    val runAs: Boolean
    val clientId: String?
    val clientVersion: String?
}