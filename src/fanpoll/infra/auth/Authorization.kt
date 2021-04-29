/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.auth

import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.openapi.schema.component.support.SecurityScheme
import fanpoll.infra.utils.IdentifiableObject
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.auth.Authentication
import io.ktor.auth.authentication
import io.ktor.routing.*
import io.ktor.util.KtorExperimentalAPI
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object Authorization {

    private val validateBlocks: MutableList<(principal: MyPrincipal, call: ApplicationCall) -> Unit> = mutableListOf()

    fun addValidateBlock(block: (principal: MyPrincipal, call: ApplicationCall) -> Unit) = validateBlocks.add(block)

    fun validate(principal: MyPrincipal, call: ApplicationCall) {
        validateBlocks.forEach { it(principal, call) }
    }
}

class AuthorizationRouteSelector(private val names: List<String?>, val principalAuths: List<PrincipalAuth>) :
    RouteSelector(RouteSelectorEvaluation.qualityTransparent) {

    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation(true, RouteSelectorEvaluation.qualityTransparent)
    }

    override fun toString(): String = "(authenticate ${names.joinToString { it ?: "\"default\"" }})"
}

@OptIn(KtorExperimentalAPI::class)
fun Route.authorize(
    vararg principalAuths: PrincipalAuth,
    optional: Boolean = false,
    build: Route.() -> Unit
): Route {
    val configurationNames = principalAuths.map { it.id }.toMutableList()
    val userAuthIndex = principalAuths.indexOfFirst { it is PrincipalAuth.User && it.allowRunAs }
    if (userAuthIndex != -1) {
        configurationNames.add(userAuthIndex, RunAsAuthProviderConfig.providerName)
    }

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
                Authorization.validate(principal, call)
                logger.debug("$principal authenticated")
            }
        }
    }
    authenticatedRoute.build()
    return authenticatedRoute
}

sealed class PrincipalAuth(override val id: String, val securitySchemes: List<SecurityScheme>) : IdentifiableObject<String>() {

    abstract fun allow(principal: MyPrincipal, call: ApplicationCall): Boolean

    class Service(
        providerName: String,
        securitySchemes: List<SecurityScheme>,
        val sourceRoleMap: Map<PrincipalSource, ServiceRole>,
        private val allowPredicate: ((ServicePrincipal, ApplicationCall) -> Boolean)? = null
    ) : PrincipalAuth(providerName, securitySchemes) {

        override fun allow(principal: MyPrincipal, call: ApplicationCall): Boolean {
            return if (principal is ServicePrincipal) {
                if (!sourceRoleMap.containsKey(principal.source)) return false
                if (sourceRoleMap[principal.source] != principal.role) return false
                allowPredicate?.invoke(principal, call) ?: true
            } else false
        }

        override fun toString(): String {
            return id + " => " + sourceRoleMap.map { it.key.name + " => " + it.value.name }
        }

        companion object {

            fun public(providerName: String, securitySchemes: List<SecurityScheme>, sources: Set<PrincipalSource>): Service =
                Service(providerName, securitySchemes, sources.associateWith { ServiceRole.Public })

            fun private(providerName: String, securitySchemes: List<SecurityScheme>, sources: Set<PrincipalSource>): Service =
                Service(providerName, securitySchemes, sources.associateWith { ServiceRole.Private })
        }
    }

    class User(
        providerName: String,
        securitySchemes: List<SecurityScheme>,
        val typeRolesMap: Map<UserType, Set<UserRole>?>,
        val allowSources: Set<PrincipalSource>,
        private val allowPredicate: ((UserPrincipal) -> Boolean)? = null,
        val allowRunAs: Boolean = true
    ) : PrincipalAuth(providerName, securitySchemes) {

        override fun allow(principal: MyPrincipal, call: ApplicationCall): Boolean {
            return if (principal is UserPrincipal) {
                if (!allowSources.contains(principal.source)) return false
                if (!typeRolesMap.containsKey(principal.userType)) return false
                val roles = typeRolesMap[principal.userType]
                if (roles.isNullOrEmpty()) return true
                if (principal.roles.isNullOrEmpty() || principal.roles.none { it in roles }) return false
                allowPredicate?.invoke(principal) ?: true
            } else false
        }

        override fun toString(): String {
            return id + " => " + typeRolesMap.map { it.key.name + " => " + (it.value?.joinToString(",") ?: "All") }
        }
    }
}

