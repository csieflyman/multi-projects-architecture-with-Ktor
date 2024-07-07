/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.auth.login

import fanpoll.ops.user.domain.User

interface UserLoginRepository {

    suspend fun findUserByAccount(account: String): User?
}