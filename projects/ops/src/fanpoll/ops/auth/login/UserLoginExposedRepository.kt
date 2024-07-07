/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.auth.login

import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.singleOrNull
import fanpoll.ops.database.exposed.OpsExposedRepositoryComponent
import fanpoll.ops.user.domain.User
import fanpoll.ops.user.repository.exposed.UserTable

class UserLoginExposedRepository : OpsExposedRepositoryComponent(), UserLoginRepository {

    override suspend fun findUserByAccount(account: String): User? {
        return dbExecute(opsDatabase) {
            UserTable
                .select(UserTable.id, UserTable.account, UserTable.enabled, UserTable.password, UserTable.roles)
                .where { UserTable.account eq account }
                .singleOrNull(User::class)
        }
    }
}