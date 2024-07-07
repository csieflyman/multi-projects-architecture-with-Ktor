/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.repository

import fanpoll.club.club.domain.Club

interface ClubRepository {

    suspend fun create(club: Club)
    suspend fun update(club: Club)
    suspend fun getById(clubId: String): Club
}