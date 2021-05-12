/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.infra.*
import fanpoll.infra.auth.ServiceAuth.API_KEY_HEADER_NAME
import fanpoll.infra.auth.ServiceAuth.ATTRIBUTE_KEY_SOURCE
import fanpoll.infra.login.SessionService
import fanpoll.infra.login.UserSession
import fanpoll.infra.utils.json
import io.ktor.application.call
import io.ktor.auth.SessionAuthenticationProvider
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.sessions.SessionSerializer
import io.ktor.sessions.SessionStorage
import io.ktor.util.AttributeKey
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel

object SessionAuth {

    const val SESSION_ID_HEADER_NAME = "sid"
}

object SessionAuthConfig {

    const val providerName = "user"

    private val ATTRIBUTE_KEY_USER_SESSION_AUTH_ERROR_CODE = AttributeKey<ResponseCode>("userSessionAuthErrorCode")

    fun configure(): SessionAuthenticationProvider.Configuration<UserPrincipal>.() -> Unit {

        return {
            validate { principal ->
                val apiKey = request.header(API_KEY_HEADER_NAME)
                val authConfig = principal.source.getConfig() as UserAuthConfig
                when {
                    authConfig.publicKey != apiKey -> {
                        attributes.put(ATTRIBUTE_KEY_USER_SESSION_AUTH_ERROR_CODE, ResponseCode.AUTH_BAD_KEY)
                        null
                    }
                    authConfig.id != principal.source.id -> {
                        attributes.put(ATTRIBUTE_KEY_USER_SESSION_AUTH_ERROR_CODE, ResponseCode.AUTH_BAD_SOURCE)
                        null
                    }
                    else -> {
                        attributes.put(ATTRIBUTE_KEY_SOURCE, principal.source)
                        SessionService.extendExpireTimeIfNeed(principal.session!!)
                        principal
                    }
                }
            }

            challenge {
                val errorCode = call.attributes.getOrNull(ATTRIBUTE_KEY_USER_SESSION_AUTH_ERROR_CODE)
                if (errorCode != null) {
                    call.respond(CodeResponseDTO(errorCode))
                } else {
                    if (call.request.path().endsWith("/logout"))
                        call.respond(CodeResponseDTO.OK)
                    else
                        call.respond(ErrorResponseDTO(RequestException(ResponseCode.AUTH_SESSION_NOT_FOUND), call))
                }
            }
        }

    }

    val jsonSessionSerializer = object : SessionSerializer<UserPrincipal> {

        override fun deserialize(text: String): UserPrincipal =
            json.decodeFromString(UserSession.Value.serializer(), text).principal()

        override fun serialize(session: UserPrincipal): String =
            json.encodeToString(UserSession.Value.serializer(), session.session!!.value)
    }

    val cacheSessionStorage = object : SessionStorage {

        override suspend fun invalidate(id: String) {
            // CUSTOMIZATION
            // we want to controller session invalidation by ourself
            //SessionService.invalidateSession(id)
        }

        override suspend fun <R> read(id: String, consumer: suspend (ByteReadChannel) -> R): R {
            return SessionService.getSessionAsByteArray(id)?.let { data -> consumer(ByteReadChannel(data)) }
                ?: throw NoSuchElementException("Session $id not found")
        }

        override suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit) {
            // CUSTOMIZATION
            // ktor call this function when update/remove session at ApplicationSendPipeline.Before for each call
            // => see io.ktor.sessions.Sessions feature
            // but we want to control session write by ourself
        }
    }
}