/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.provider

import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.login.session.MySessionStorage
import fanpoll.infra.auth.login.session.SessionConfig
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.response.respond
import io.ktor.application.call
import io.ktor.auth.SessionAuthenticationProvider
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.util.AttributeKey

data class UserSessionAuthConfig(
    val principalSource: PrincipalSource,
    val apiKey: String? = null,
    var session: SessionConfig? = null
)

private val ATTRIBUTE_KEY_AUTH_ERROR_CODE = AttributeKey<ResponseCode>("AuthErrorCode")

class UserSessionAuthValidator(private val authConfigs: List<UserSessionAuthConfig>, private val sessionStorage: MySessionStorage) {

    val configureFunction: SessionAuthenticationProvider.Configuration<UserPrincipal>.() -> Unit = {
        validate { principal ->
            val apiKey = request.header(AuthConst.API_KEY_HEADER_NAME)

            val authConfig = authConfigs.firstOrNull { it.principalSource == principal.source }
            if (authConfig != null) {
                if (authConfig.apiKey != null && authConfig.apiKey != apiKey) {
                    attributes.put(ATTRIBUTE_KEY_AUTH_ERROR_CODE, InfraResponseCode.AUTH_BAD_KEY)
                    null
                } else {
                    attributes.put(PrincipalSource.ATTRIBUTE_KEY, principal.source)
                    sessionStorage.extendExpireTime(principal.session!!)
                    principal
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
                    call.respond(CodeResponseDTO.OK)
                else
                    call.respond(CodeResponseDTO(InfraResponseCode.AUTH_SESSION_NOT_FOUND))
            }
        }
    }
}