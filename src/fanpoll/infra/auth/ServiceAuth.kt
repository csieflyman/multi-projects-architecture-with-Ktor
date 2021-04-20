/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.auth

import fanpoll.infra.*
import fanpoll.infra.auth.ServiceAuth.API_KEY_HEADER_NAME
import fanpoll.infra.auth.ServiceAuth.ATTRIBUTE_KEY_SOURCE
import fanpoll.infra.controller.fromLocalhost
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.features.origin
import io.ktor.request.header
import io.ktor.util.AttributeKey

object ServiceAuth {

    const val API_KEY_HEADER_NAME = "X-API-KEY"

    val ATTRIBUTE_KEY_SOURCE = AttributeKey<PrincipalSource>("source")
}

object ServiceAuthProviderConfig {

    class ApiKeyProvider constructor(config: Configuration) : AuthenticationProvider(config) {

        private val authConfigs: List<PrincipalSourceAuthConfig<ServiceAuthApiKeyCredential>> = config.authConfigs

        val authenticationFunction: AuthenticationFunction<ServiceAuthApiKeyCredential> = { credentials ->

            var principal: MyPrincipal? = null
            run loop@{
                authConfigs.forEach { authConfig ->
                    principal = authConfig.authenticate(credentials)
                    if (principal != null) return@loop
                }
            }
            if (principal != null) {
                attributes.put(ATTRIBUTE_KEY_SOURCE, principal!!.source)
            }
            principal
        }

        class Configuration constructor(
            providerName: String,
            val authConfigs: List<PrincipalSourceAuthConfig<ServiceAuthApiKeyCredential>>
        ) : AuthenticationProvider.Configuration(providerName) {

            fun build() = ApiKeyProvider(this)
        }
    }

    class HostProvider constructor(config: Configuration) : AuthenticationProvider(config) {

        private val authConfig: ServiceHostAuthConfig = config.authConfig

        val authenticationFunction: AuthenticationFunction<ServiceAuthHostCredential> = { credentials ->
            val principal = if (request.fromLocalhost()) {
                ServicePrincipal(PrincipalSource.lookup(authConfig.id), ServiceRole.Private)
            } else {
                authConfig.authenticate(credentials)
            }

            if (principal != null) {
                attributes.put(ATTRIBUTE_KEY_SOURCE, principal.source)
            }
            principal
        }

        class Configuration constructor(providerName: String, val authConfig: ServiceHostAuthConfig) :
            AuthenticationProvider.Configuration(providerName) {

            fun build() = HostProvider(this)
        }
    }
}

data class ServiceAuthApiKeyCredential(val apiKey: String) : Credential

fun Authentication.Configuration.serviceApiKey(
    providerName: String,
    serviceAuthConfigs: List<PrincipalSourceAuthConfig<ServiceAuthApiKeyCredential>>
) {

    val provider = ServiceAuthProviderConfig.ApiKeyProvider.Configuration(providerName, serviceAuthConfigs).build()

    provider.pipeline.intercept(AuthenticationPipeline.CheckAuthentication) { context ->
        val apiKey = call.request.header(API_KEY_HEADER_NAME)

        val credentials = if (apiKey != null) ServiceAuthApiKeyCredential(apiKey) else null
        val principal = credentials?.let { (provider.authenticationFunction)(call, it) as ServicePrincipal? }

        if (principal != null) {
            context.principal(principal)
        } else {
            val cause = if (credentials == null) AuthenticationFailedCause.NoCredentials
            else AuthenticationFailedCause.InvalidCredentials

            context.challenge(providerName, cause) {
                call.respondMyResponse(HttpStatusResponse(ResponseCode.AUTH_BAD_KEY))
                it.complete()
            }
        }
    }

    register(provider)
}

data class ServiceAuthHostCredential(val host: String) : Credential

fun Authentication.Configuration.serviceAllowHosts(providerName: String, authConfig: ServiceHostAuthConfig) {

    val provider = ServiceAuthProviderConfig.HostProvider.Configuration(providerName, authConfig).build()

    provider.pipeline.intercept(AuthenticationPipeline.CheckAuthentication) { context ->
        val host = call.request.origin.remoteHost

        val credentials = ServiceAuthHostCredential(host)
        val principal = credentials.let { (provider.authenticationFunction)(call, it) as ServicePrincipal? }

        if (principal != null) {
            context.principal(principal)
        } else {
            context.challenge(providerName, AuthenticationFailedCause.InvalidCredentials) {
                call.respondMyResponse(ErrorResponse(RequestException(ResponseCode.AUTH_BAD_HOST, "host = $host"), call))
                it.complete()
            }
        }
    }

    register(provider)
}