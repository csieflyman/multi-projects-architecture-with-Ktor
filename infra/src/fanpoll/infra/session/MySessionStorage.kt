/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.session

import io.ktor.server.sessions.SessionStorage

abstract class MySessionStorage : SessionStorage {

    override suspend fun invalidate(id: String) {
        // CUSTOMIZATION
        // we want to controller session invalidation by ourself
        //SessionService.invalidateSession(id)
    }

    override suspend fun read(id: String): String = getSessionAsText(id) ?: throw NoSuchElementException("Session $id not found")

    override suspend fun write(id: String, value: String) {
        // CUSTOMIZATION
        // ktor call this function when update/remove session at ApplicationSendPipeline.Before for each call
        // => see io.ktor.sessions.Sessions plugin
        // but we want to control session write by ourself
    }

    abstract suspend fun setSession(session: UserSession)

    abstract suspend fun setSessionExpireTime(session: UserSession)

    abstract suspend fun deleteSession(session: UserSession)

    abstract suspend fun getSession(sid: String): UserSession?

    abstract suspend fun getSessionAsText(sid: String): String?

    abstract suspend fun hasSession(sid: String): Boolean
}