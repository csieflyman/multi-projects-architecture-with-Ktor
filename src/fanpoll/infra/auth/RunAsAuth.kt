/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.RunAsAuth.RUN_AS_TOKEN_HEADER_NAME
import fanpoll.infra.auth.ServiceAuth.API_KEY_HEADER_NAME
import fanpoll.infra.auth.ServiceAuth.ATTRIBUTE_KEY_SOURCE
import fanpoll.infra.login.UserSession
import fanpoll.infra.utils.json
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.request.header
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import java.time.Instant
import java.util.*

data class RunAsAuthCredential(val apiKey: String, val runAsToken: String) : Credential

object RunAsAuth {

    const val RUN_AS_TOKEN_HEADER_NAME = "runAs"
}

object RunAsAuthProviderConfig {

    private val logger = KotlinLogging.logger {}

    const val providerName = "runAs"

    const val tokenPatternDescription = "userType-userId-clientId-sessionData(jsonObjectString)"

    class Provider constructor(config: Configuration) : AuthenticationProvider(config) {

        private val authConfigs: List<UserAuthConfig> = PrincipalSource.getRunAsConfigs()

        val authenticationFunction: AuthenticationFunction<RunAsAuthCredential> = { credentials ->

            var principal: UserPrincipal? = null
            run loop@{
                authConfigs.forEach { authConfig ->
                    if (authConfig.runAsKey == credentials.apiKey) {
                        val token = parseRunAsToken(credentials.runAsToken)
                        val user = token.userType.findRunAsUserById(token.userId)
                        principal = UserPrincipal(
                            token.userType, token.userId,
                            user.roles, PrincipalSource.lookup(authConfig.id),
                            token.clientId, user.tenantId, true
                        )
                        if (principal != null && token.sessionData != null)
                            principal!!.session = UserSession(principal!!, Instant.now(), null, token.sessionData)
                        return@loop
                    }
                }
            }
            if (principal != null) {
                attributes.put(ATTRIBUTE_KEY_SOURCE, principal!!.source)
            }
            principal
        }

        class Configuration : AuthenticationProvider.Configuration(providerName) {

            fun build() = Provider(this)
        }
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
            throw RequestException(ResponseCode.REQUEST_BAD_HEADER, "invalid runAs token value format => $tokenPatternDescription")
        }
    }

    private class RunAsToken(
        val userType: UserType,
        val userId: UUID,
        val clientId: String?,
        val sessionData: JsonObject? = null
    )
}

fun Authentication.Configuration.runAs() {

    val provider = RunAsAuthProviderConfig.Provider.Configuration().build()

    provider.pipeline.intercept(AuthenticationPipeline.CheckAuthentication) { context ->
        val apiKey = call.request.header(API_KEY_HEADER_NAME)
        val runAsToken = call.request.header(RUN_AS_TOKEN_HEADER_NAME)

        if (apiKey != null && runAsToken != null) {
            val principal = (provider.authenticationFunction)(call, RunAsAuthCredential(apiKey, runAsToken)) as UserPrincipal?
            if (principal != null) {
                context.principal(principal)
            }
        }
    }

    register(provider)
}