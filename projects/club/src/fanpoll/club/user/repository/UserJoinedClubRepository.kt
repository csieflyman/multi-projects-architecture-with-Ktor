/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.repository

import fanpoll.club.user.domain.UserJoinedClub
import java.util.*

interface UserJoinedClubRepository {

    suspend fun joinClub(userJoinedClub: UserJoinedClub)
    suspend fun getJoinedClubs(userId: UUID, fetchClub: Boolean = false): List<UserJoinedClub>
    suspend fun getJoinedClub(userId: UUID, clubId: String, fetchClub: Boolean = false): UserJoinedClub?

}