/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.auth

import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.ResponseCode
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.auth.Authentication
import io.ktor.auth.authentication
import io.ktor.routing.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AuthorizationRouteSelector(private val names: List<String?>, val principalAuths: List<PrincipalAuth>) : RouteSelector() {

    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation(true, RouteSelectorEvaluation.qualityTransparent)
    }

    override fun toString(): String = "(authenticate ${names.joinToString { it ?: "\"default\"" }})"
}

fun Route.authorize(
    vararg principalAuths: PrincipalAuth,
    optional: Boolean = false,
    build: Route.() -> Unit
): Route {
    val configurationNames = principalAuths.map { it.id }.toMutableList()
    val runAsProviderNames = principalAuths.mapNotNull {
        if (it is PrincipalAuth.User && it.runAsAuthProviderName != null) it.runAsAuthProviderName else null
    }
    configurationNames.plus(runAsProviderNames)

    val authenticatedRoute = createChild(AuthorizationRouteSelector(configurationNames, principalAuths.toList()))

    application.feature(Authentication).interceptPipeline(authenticatedRoute, configurationNames, optional = optional)
    authenticatedRoute.intercept(Authentication.ChallengePhase) {
        val principal = call.authentication.principal

        if (principal == null) {
            if (call.response.status() != null) finish()
            else error("principal is null and no response in authorize challenge phase")
        } else {
            require(principal is MyPrincipal)

            if (principalAuths.none { it.allow(principal, call) }) {
                throw RequestException(ResponseCode.AUTH_ROLE_FORBIDDEN, "$principal is forbidden unable to access this api")
            } else {
                logger.debug("$principal authenticated")
            }
        }
    }
    authenticatedRoute.build()
    return authenticatedRoute
}

