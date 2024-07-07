/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.repository

import fanpoll.club.user.domain.User
import java.util.*

interface UserRepository {

    suspend fun create(user: User): UUID
    suspend fun update(user: User)
    suspend fun getById(userId: UUID): User
}