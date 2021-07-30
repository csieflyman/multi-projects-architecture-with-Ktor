/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.session

interface SessionService {

    suspend fun setSession(session: UserSession)

    suspend fun deleteSession(session: UserSession)

    suspend fun extendExpireTime(session: UserSession)

    suspend fun getSession(sid: String): UserSession?

    suspend fun getSessionAsByteArray(sid: String): ByteArray?

    suspend fun hasSession(sid: String): Boolean
}