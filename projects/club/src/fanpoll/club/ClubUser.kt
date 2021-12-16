/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.features.ClubUserTable
import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.auth.provider.RunAsUser
import fanpoll.infra.database.sql.transaction
import org.jetbrains.exposed.sql.select
import java.util.*

enum class ClubUserType(val value: UserType) {

    User(object : UserType(ClubConst.projectId, "user") {

        override val roles: Set<UserRole> = setOf(UserRole(id, "admin"), UserRole(id, "member"))

        override fun findRunAsUserById(userId: UUID): RunAsUser {
            val row = transaction {
                ClubUserTable.select { ClubUserTable.id eq userId }.single()
            }
            return RunAsUser(userId, this, setOf(row[ClubUserTable.role].value))
        }
    })
}

enum class ClubUserRole(val value: UserRole) {

    Admin(ClubUserType.User.value.roles!!.first { it.name == "admin" }),
    Member(ClubUserType.User.value.roles!!.first { it.name == "member" })
}