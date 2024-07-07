/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.provider

import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.session.UserSession
import io.ktor.server.auth.*
import io.ktor.server.request.header
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.util.*

data class UserRunAsAuthCredential(val runAsKey: String, val runAsToken: String) : Credential

data class UserRunAsToken(
    val userType: UserType,
    val userId: UUID,
    val clientId: String? = null,
    val sessionData: JsonObject? = null
) {
    val value = "${userType.getId()}:$userId:${clientId ?: "?"}${sessionData?.let { ":${json.encodeToString(it)}" } ?: ""}"
}

data class UserRunAsAuthConfig(
    val principalSource: PrincipalSource,
    val runAsKey: String
)

class UserRunAsAuthProvider(config: Configuration) : AuthenticationProvider(config) {

    companion object {
        const val RUN_AS_TOKEN_HEADER_NAME = "runAs"
        const val tokenPatternDescription = "userType:userId:clientId:sessionData(jsonObjectString)"
    }

    private val authConfigs: List<UserRunAsAuthConfig> = config.authConfigs

    private val findUserByTypeIdFunction: (UserType, UUID) -> RunAsUser = config.findUserByTypeIdFunction

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val apiKey = call.request.header(AuthConst.API_KEY_HEADER_NAME)
        val runAsToken = call.request.header(RUN_AS_TOKEN_HEADER_NAME)

        if (apiKey != null && runAsToken != null) {
            val principal = (authenticationFunction)(call, UserRunAsAuthCredential(apiKey, runAsToken)) as UserPrincipal?
            if (principal != null) {
                context.principal(principal)
                call.sessions.set<UserSession>(principal as UserSession)
            }
        }
    }

    private val authenticationFunction: AuthenticationFunction<UserRunAsAuthCredential> = { credentials ->
        var session: UserSession? = null
        run loop@{
            authConfigs.forEach { runAsConfig ->
                if (runAsConfig.runAsKey == credentials.runAsKey) {
                    val token = parseRunAsToken(credentials.runAsToken)
                    val user = findUserByTypeIdFunction(token.userType, token.userId)
                    val source = runAsConfig.principalSource
                    session = UserSession(
                        user.account, source,
                        user.id, user.type, user.roles,
                        true, token.clientId, null, Instant.now(), null, token.sessionData
                    )
                    return@loop
                }
            }
        }
        if (session != null) {
            attributes.put(PrincipalSource.ATTRIBUTE_KEY, session!!.source)
        }
        session
    }

    private fun parseRunAsToken(text: String): UserRunAsToken {
        return try {
            val segments = text.split(":")
            require(segments.size in 2..3)

            val userType = UserType.getTypeById(segments[0])
            val userId = UUID.fromString(segments[1])
            val clientId = if (segments.size >= 3 && segments[2] != "?") segments[2] else null
            val sessionData = if (segments.size >= 4)
                json.parseToJsonElement(text.substring((0..2)
                    .sumOf { index -> segments[index].length } + 3)).jsonObject else null
            UserRunAsToken(userType, userId, clientId, sessionData)
        } catch (e: Exception) {
            throw RequestException(InfraResponseCode.BAD_REQUEST_HEADER, "invalid runAs token value format => $tokenPatternDescription")
        }
    }

    class Configuration(
        providerName: String,
        val authConfigs: List<UserRunAsAuthConfig>,
        val findUserByTypeIdFunction: (UserType, UUID) -> RunAsUser
    ) : Config(providerName) {

        fun build() = UserRunAsAuthProvider(this)
    }
}

fun AuthenticationConfig.runAs(
    providerName: String,
    authConfigs: List<UserRunAsAuthConfig>,
    findUserByTypeIdFunction: (UserType, UUID) -> RunAsUser
) {
    val provider = UserRunAsAuthProvider.Configuration(providerName, authConfigs, findUserByTypeIdFunction).build()
    register(provider)
}

data class RunAsUser(
    override val id: UUID,
    val account: String,
    val type: UserType,
    val roles: Set<UserRole>
) : IdentifiableObject<UUID>()