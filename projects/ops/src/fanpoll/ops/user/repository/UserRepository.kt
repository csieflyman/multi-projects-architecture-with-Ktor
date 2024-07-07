/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.repository

import fanpoll.ops.user.domain.User
import java.util.*

interface UserRepository {
    suspend fun create(user: User): UUID
    suspend fun update(user: User)
    suspend fun getById(userId: UUID): User
}



