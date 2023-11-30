/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.auth.provider

import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.ServicePrincipal
import fanpoll.infra.base.extension.fromLocalhost
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.response.respond
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.util.AttributeKey

data class ServiceAuthCredential(val apiKey: String, val host: String) : Credential

data class ServiceAuthConfig(
    val principalSource: PrincipalSource,
    val apiKey: String,
    val allowHosts: String? = null
)

private val ATTRIBUTE_KEY_AUTH_ERROR_CODE = AttributeKey<ResponseCode>("AuthErrorCode")

class ServiceAuthProvider(config: Configuration) : AuthenticationProvider(config) {

    private val logger = KotlinLogging.logger {}
    private val providerName: String = config.providerName
    private val authConfigs: List<ServiceAuthConfig> = config.authConfigs

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val apiKey = call.request.header(AuthConst.API_KEY_HEADER_NAME)
        val host = call.request.origin.remoteHost

        val credentials = if (apiKey != null) ServiceAuthCredential(apiKey, host) else null
        val principal = credentials?.let { (authenticationFunction)(call, it) as ServicePrincipal? }

        if (principal != null) {
            context.principal(principal)
        } else {
            val cause = if (credentials == null) AuthenticationFailedCause.NoCredentials
            else AuthenticationFailedCause.InvalidCredentials

            context.challenge(providerName, cause) { challenge, _ ->
                call.respond(
                    CodeResponseDTO(
                        if (credentials == null) InfraResponseCode.AUTH_BAD_KEY
                        else call.attributes[ATTRIBUTE_KEY_AUTH_ERROR_CODE]
                    )
                )
                challenge.complete()
            }
        }
    }

    private val authenticationFunction: AuthenticationFunction<ServiceAuthCredential> = { credential ->

        val authConfig = authConfigs.firstOrNull {
            credential.apiKey == it.apiKey
        }
        if (authConfig != null) {
            attributes.put(PrincipalSource.ATTRIBUTE_KEY, authConfig.principalSource)

            val hostAllowed = if (authConfig.allowHosts == null || authConfig.allowHosts == "*" || request.fromLocalhost()) true
            else authConfig.allowHosts.split(",").any { it == credential.host }

            if (hostAllowed) {
                ServicePrincipal(authConfig.principalSource)
            } else {
                attributes.put(ATTRIBUTE_KEY_AUTH_ERROR_CODE, InfraResponseCode.AUTH_BAD_HOST)
                null
            }
        } else {
            attributes.put(ATTRIBUTE_KEY_AUTH_ERROR_CODE, InfraResponseCode.AUTH_BAD_KEY)
            null
        }
    }

    class Configuration constructor(val providerName: String, val authConfigs: List<ServiceAuthConfig>) : Config(providerName) {

        fun build(): ServiceAuthProvider = ServiceAuthProvider(this)
    }
}

fun AuthenticationConfig.service(providerName: String, authConfigs: List<ServiceAuthConfig>) {
    val provider = ServiceAuthProvider.Configuration(providerName, authConfigs).build()
    register(provider)
}