/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.repository.exposed

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.dao.ExposedDaoRepository
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.ops.database.exposed.OpsExposedRepositoryComponent
import fanpoll.ops.user.domain.User
import fanpoll.ops.user.repository.UserRepository
import org.jetbrains.exposed.sql.selectAll
import java.util.*

class UserExposedRepository : OpsExposedRepositoryComponent(), UserRepository {

    private val exposedDaoRepository: ExposedDaoRepository<User, UUID> =
        ExposedDaoRepository(User::class, UserEntity, opsDatabase)

    override suspend fun create(user: User): UUID {
        return dbExecute(opsDatabase) {
            if (UserTable.selectAll().where { UserTable.account eq user.account!! }.count() > 0)
                throw RequestException(InfraResponseCode.ENTITY_ALREADY_EXISTS, "${user.account} already exists")
            exposedDaoRepository.createAndGetId(user)
        }
    }

    override suspend fun update(user: User) {
        exposedDaoRepository.update(user)
    }

    override suspend fun getById(userId: UUID): User {
        return exposedDaoRepository.getById(userId)
    }
}