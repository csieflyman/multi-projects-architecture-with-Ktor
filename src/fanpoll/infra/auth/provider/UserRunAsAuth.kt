/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.provider

import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.login.session.UserSession
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.auth.provider.UserRunAsAuthProvider.Companion.RUN_AS_TOKEN_HEADER_NAME
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.tenant.TenantId
import fanpoll.infra.base.util.IdentifiableObject
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.request.header
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.*

data class UserRunAsAuthCredential(val runAsKey: String, val runAsToken: String) : Credential

data class UserRunAsAuthConfig(
    val principalSource: PrincipalSource,
    val runAsKey: String
)

class UserRunAsAuthProvider(config: Configuration) : AuthenticationProvider(config) {

    companion object {
        const val RUN_AS_TOKEN_HEADER_NAME = "runAs"
        const val tokenPatternDescription = "userType-userId-clientId-sessionData(jsonObjectString)"
    }

    private val authConfigs: List<UserRunAsAuthConfig> = config.authConfigs

    val authenticationFunction: AuthenticationFunction<UserRunAsAuthCredential> = { credentials ->

        var principal: UserPrincipal? = null
        run loop@{
            authConfigs.forEach { runAsConfig ->
                if (runAsConfig.runAsKey == credentials.runAsKey) {
                    val token = parseRunAsToken(credentials.runAsToken)
                    val user = token.userType.findRunAsUserById(token.userId)
                    principal = UserPrincipal(
                        token.userType, token.userId,
                        user.roles, runAsConfig.principalSource,
                        token.clientId, user.tenantId, true
                    )
                    if (principal != null && token.sessionData != null)
                        principal!!.session = UserSession(principal!!, null, token.sessionData)
                    return@loop
                }
            }
        }
        if (principal != null) {
            attributes.put(PrincipalSource.ATTRIBUTE_KEY, principal!!.source)
        }
        principal
    }

    private fun parseRunAsToken(text: String): RunAsToken {
        return try {
            val segments = text.split("-")
            require(segments.size in 2..3)

            val userType = UserType.lookup(segments[0])
            val userId = UUID.fromString(segments[1])
            val clientId = if (segments.size >= 3 && segments[2] != "?") segments[2] else null
            val sessionData = if (segments.size >= 4)
                json.parseToJsonElement(text.substring((0..2)
                    .sumOf { index -> segments[index].length } + 3)).jsonObject else null
            RunAsToken(userType, userId, clientId, sessionData)
        } catch (e: Exception) {
            throw RequestException(ResponseCode.BAD_REQUEST_HEADER, "invalid runAs token value format => $tokenPatternDescription")
        }
    }

    private class RunAsToken(
        val userType: UserType,
        val userId: UUID,
        val clientId: String?,
        val sessionData: JsonObject? = null
    )

    class Configuration constructor(providerName: String, val authConfigs: List<UserRunAsAuthConfig>) :
        AuthenticationProvider.Configuration(providerName) {

        fun build() = UserRunAsAuthProvider(this)
    }
}

fun Authentication.Configuration.runAs(providerName: String, authConfigs: List<UserRunAsAuthConfig>) {

    val provider = UserRunAsAuthProvider.Configuration(providerName, authConfigs).build()

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val apiKey = call.request.header(AuthConst.API_KEY_HEADER_NAME)
        val runAsToken = call.request.header(RUN_AS_TOKEN_HEADER_NAME)

        if (apiKey != null && runAsToken != null) {
            val principal = (provider.authenticationFunction)(call, UserRunAsAuthCredential(apiKey, runAsToken)) as UserPrincipal?
            if (principal != null) {
                context.principal(principal)
            }
        }
    }

    register(provider)
}

data class RunAsUser(
    override val id: UUID,
    val type: UserType,
    val roles: Set<UserRole>? = null,
    val tenantId: TenantId? = null
) : IdentifiableObject<UUID>()