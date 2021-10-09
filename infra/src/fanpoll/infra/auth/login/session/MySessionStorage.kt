/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.session

import io.ktor.sessions.SessionStorage
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel

abstract class MySessionStorage : SessionStorage {

    override suspend fun invalidate(id: String) {
        // CUSTOMIZATION
        // we want to controller session invalidation by ourself
        //SessionService.invalidateSession(id)
    }

    override suspend fun <R> read(id: String, consumer: suspend (ByteReadChannel) -> R): R {
        return getSessionAsByteArray(id)?.let { data -> consumer(ByteReadChannel(data)) }
            ?: throw NoSuchElementException("Session $id not found")
    }

    override suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit) {
        // CUSTOMIZATION
        // ktor call this function when update/remove session at ApplicationSendPipeline.Before for each call
        // => see io.ktor.sessions.Sessions feature
        // but we want to control session write by ourself
    }

    abstract suspend fun setSession(session: UserSession)

    abstract suspend fun deleteSession(session: UserSession)

    abstract suspend fun extendExpireTime(session: UserSession)

    abstract suspend fun getSession(sid: String): UserSession?

    abstract suspend fun getSessionAsByteArray(sid: String): ByteArray?

    abstract suspend fun hasSession(sid: String): Boolean
}