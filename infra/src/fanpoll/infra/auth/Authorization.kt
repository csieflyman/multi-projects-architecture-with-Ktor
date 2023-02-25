/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.auth

import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

val AuthorizedRoutePrincipalAuthsKey = AttributeKey<List<PrincipalAuth>>("AuthorizedRoutePrincipalAuthsKey")

fun Route.authorize(
    vararg principalAuths: PrincipalAuth,
    optional: Boolean = false,
    build: Route.() -> Unit
): Route {
    // authProviders order: runas -> service -> user
    val configurationNames = principalAuths.map { it.id }.toMutableList()
    val runAsProviderNames = principalAuths.mapNotNull {
        if (it is PrincipalAuth.User && it.runAsAuthProviderName != null) it.runAsAuthProviderName else null
    }
    configurationNames.addAll(0, runAsProviderNames)
    val principalAuthList = principalAuths.toList()

    return authenticate(*configurationNames.toTypedArray(), optional = optional) {
        install(AuthorizationPlugin) {
            this.principalAuths = principalAuthList
        }
        attributes.put(AuthorizedRoutePrincipalAuthsKey, principalAuthList)
        build()
    }
}

val AuthorizationPlugin = createRouteScopedPlugin(
    name = "AuthorizationPlugin",
    createConfiguration = ::PluginConfiguration
) {
    on(AuthenticationChecked) { call ->
        val principal = call.principal<Principal>()
        if (principal != null) {
            require(principal is MyPrincipal)

            if (pluginConfig.principalAuths.none { it.allow(principal, call) }) {
                throw RequestException(InfraResponseCode.AUTH_ROLE_FORBIDDEN, "$principal is forbidden unable to access this api")
            } else {
                logger.debug("$principal authenticated")
            }
        } else {
            require(call.response.status() != null) { "Authenticator should response status code if unauthenticated" }
        }
    }
}

class PluginConfiguration {
    lateinit var principalAuths: List<PrincipalAuth>
}
