/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.repository.exposed

import fanpoll.club.club.domain.ClubRole
import fanpoll.club.club.repository.exposed.ClubTable
import fanpoll.club.database.exposed.ClubRepositoryComponent
import fanpoll.club.user.domain.UserJoinedClub
import fanpoll.club.user.repository.UserJoinedClubRepository
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.toObject
import org.jetbrains.exposed.sql.*
import java.util.*

class ExposedUserJoinedClubRepository : ClubRepositoryComponent(), UserJoinedClubRepository {

    override suspend fun joinClub(userJoinedClub: UserJoinedClub) {
        dbExecute(clubDatabase) {
            if (UserJoinedClubTable.selectAll()
                    .where { (UserJoinedClubTable.clubId eq userJoinedClub.clubId) and (UserJoinedClubTable.userId eq userJoinedClub.userId) }
                    .count() > 0
            )
                throw RequestException(
                    InfraResponseCode.ENTITY_ALREADY_EXISTS,
                    "${userJoinedClub.userId} already join club ${userJoinedClub.clubId}"
                )

            UserJoinedClubTable.insert {
                it[userId] = userJoinedClub.userId
                it[clubId] = userJoinedClub.clubId
                it[isAdmin] = userJoinedClub.role != ClubRole.Member
            }
        }
    }

    override suspend fun getJoinedClubs(userId: UUID, fetchClub: Boolean): List<UserJoinedClub> {
        return dbExecute(clubDatabase) {
            UserJoinedClubTable.selectAll()
                .where { UserJoinedClubTable.userId eq userId }
                .apply {
                    if (fetchClub)
                        this.adjustColumnSet { innerJoin(ClubTable.alias("club")) }.adjustSelect { select(ClubTable.columns) }
                }
                .toList().map { resultRowToUserJoinedClub(it) }
        }
    }

    override suspend fun getJoinedClub(userId: UUID, clubId: String, fetchClub: Boolean): UserJoinedClub? {
        return dbExecute(clubDatabase) {
            UserJoinedClubTable.selectAll()
                .where { (UserJoinedClubTable.userId eq userId) and (UserJoinedClubTable.clubId eq clubId) }
                .apply {
                    if (fetchClub)
                        this.adjustColumnSet { innerJoin(ClubTable.alias("club")) }.adjustSelect { select(ClubTable.columns) }
                }
                .singleOrNull()?.let { resultRowToUserJoinedClub(it) }
        }
    }

    private fun resultRowToUserJoinedClub(resultRow: ResultRow): UserJoinedClub {
        return resultRow.toObject(UserJoinedClub::class)!!.apply {
            role = if (resultRow[UserJoinedClubTable.isAdmin]) ClubRole.Admin else ClubRole.Member
        }
    }
}