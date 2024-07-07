/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.services

import fanpoll.club.user.domain.UserJoinedClub
import fanpoll.club.user.repository.UserJoinedClubRepository
import java.util.*

class UserJoinClubService(private val userJoinedClubRepository: UserJoinedClubRepository) {

    suspend fun joinClub(userJoinedClub: UserJoinedClub) {
        userJoinedClubRepository.joinClub(userJoinedClub)
    }

    suspend fun getJoinedClubs(userId: UUID, fetchClub: Boolean = false): List<UserJoinedClub> {
        return userJoinedClubRepository.getJoinedClubs(userId, fetchClub)
    }

    suspend fun getJoinedClub(userId: UUID, clubId: String, fetchClub: Boolean = false): UserJoinedClub? {
        return userJoinedClubRepository.getJoinedClub(userId, clubId, fetchClub)
    }
}