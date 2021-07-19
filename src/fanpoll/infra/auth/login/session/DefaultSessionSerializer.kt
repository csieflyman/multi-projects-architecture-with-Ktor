/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.session

import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.json.json
import io.ktor.sessions.SessionSerializer

class DefaultSessionSerializer : SessionSerializer<UserPrincipal> {

    override fun deserialize(text: String): UserPrincipal =
        json.decodeFromString(UserSession.Value.serializer(), text).principal()

    override fun serialize(session: UserPrincipal): String =
        json.encodeToString(UserSession.Value.serializer(), session.session!!.value)
}