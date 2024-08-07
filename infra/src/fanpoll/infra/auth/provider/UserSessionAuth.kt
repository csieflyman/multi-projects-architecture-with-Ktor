/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.provider

import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.response.respond
import fanpoll.infra.session.MySessionStorage
import fanpoll.infra.session.SessionConfig
import fanpoll.infra.session.UserSession
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.SessionAuthenticationProvider
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import java.time.Duration
import java.time.Instant

data class UserSessionAuthConfig(
    val principalSource: PrincipalSource,
    val apiKey: String? = null,
    var session: SessionConfig? = null
)

private val ATTRIBUTE_KEY_AUTH_ERROR_CODE = AttributeKey<ResponseCode>("AuthErrorCode")

class UserSessionAuthValidator(private val authConfigs: List<UserSessionAuthConfig>, private val sessionStorage: MySessionStorage) {

    private val logger = KotlinLogging.logger {}

    val configureFunction: SessionAuthenticationProvider.Config<UserSession>.() -> Unit = {
        validate { session ->
            val apiKey = request.header(AuthConst.API_KEY_HEADER_NAME)

            val authConfig = authConfigs.firstOrNull { it.principalSource == session.source }
            if (authConfig != null) {
                if (authConfig.apiKey != null && authConfig.apiKey != apiKey) {
                    attributes.put(ATTRIBUTE_KEY_AUTH_ERROR_CODE, InfraResponseCode.AUTH_BAD_KEY)
                    null
                } else {
                    attributes.put(PrincipalSource.ATTRIBUTE_KEY, session.source)

                    val sessionConfig = session.source.getAuthConfig().user!!.session
                    val now = Instant.now()
                    if (sessionConfig?.extendDuration != null && session.expireTime != null &&
                        session.expireTime!!.isAfter(now) &&
                        Duration.between(now, session.expireTime) <= sessionConfig.extendDuration
                    ) {
                        logger.debug { "extent session: ${session.sid}" }
                        sessionStorage.setSessionExpireTime(session)
                    }
                    session
                }
            } else {
                attributes.put(ATTRIBUTE_KEY_AUTH_ERROR_CODE, InfraResponseCode.AUTH_BAD_SOURCE)
                null
            }
        }

        challenge {
            val errorCode = call.attributes.getOrNull(ATTRIBUTE_KEY_AUTH_ERROR_CODE)
            if (errorCode != null) {
                call.respond(CodeResponseDTO(errorCode))
            } else {
                if (call.request.path().endsWith("/logout"))
                    call.respond(CodeResponseDTO(InfraResponseCode.AUTH_SESSION_NOT_FOUND_OR_EXPIRED))
                else
                    call.respond(CodeResponseDTO(InfraResponseCode.AUTH_SESSION_NOT_FOUND))
            }
        }
    }
}