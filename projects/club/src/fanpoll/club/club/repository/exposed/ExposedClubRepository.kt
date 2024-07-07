/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.repository.exposed

import fanpoll.club.club.domain.Club
import fanpoll.club.club.repository.ClubRepository
import fanpoll.club.database.exposed.ClubRepositoryComponent
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.dao.ExposedDaoRepository
import fanpoll.infra.database.exposed.sql.dbExecute
import org.jetbrains.exposed.sql.selectAll

class ExposedClubRepository : ClubRepositoryComponent(), ClubRepository {

    private val exposedDaoRepository: ExposedDaoRepository<Club, String> =
        ExposedDaoRepository(Club::class, ClubEntity, clubDatabase)

    override suspend fun create(club: Club) {
        dbExecute(clubDatabase) {
            if (ClubTable.selectAll().where { ClubTable.id eq club.id }.count() > 0)
                throw RequestException(InfraResponseCode.ENTITY_ALREADY_EXISTS, "${club.id} already exists")
            exposedDaoRepository.create(club)
        }
    }

    override suspend fun update(club: Club) {
        exposedDaoRepository.update(club)
    }

    override suspend fun getById(clubId: String): Club {
        return exposedDaoRepository.getById(clubId)
    }
}