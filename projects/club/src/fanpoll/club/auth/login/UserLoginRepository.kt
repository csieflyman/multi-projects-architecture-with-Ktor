/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.auth.login

import fanpoll.club.user.domain.User

interface UserLoginRepository {

    suspend fun findUserByAccount(account: String): User?
}