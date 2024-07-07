/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.auth.login

import fanpoll.club.database.exposed.ClubRepositoryComponent
import fanpoll.club.user.domain.User
import fanpoll.club.user.repository.exposed.UserTable
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.singleOrNull

class UserLoginExposedRepository : ClubRepositoryComponent(), UserLoginRepository {
    override suspend fun findUserByAccount(account: String): User? {
        return dbExecute(clubDatabase) {
            UserTable.select(UserTable.id, UserTable.account, UserTable.enabled, UserTable.password, UserTable.roles)
                .where { UserTable.account eq account }
                .singleOrNull(User::class)
        }
    }
}