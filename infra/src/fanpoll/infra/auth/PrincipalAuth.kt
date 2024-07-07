/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.infra.auth.principal.*
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.openapi.schema.component.support.SecurityScheme
import fanpoll.infra.session.UserSession
import io.ktor.server.application.ApplicationCall

sealed class PrincipalAuth(
    override val id: String,
    val securitySchemes: List<SecurityScheme>,
    val allowSources: Set<PrincipalSource>
) : IdentifiableObject<String>() {

    abstract fun allow(principal: MyPrincipal, call: ApplicationCall): Boolean

    class Service(
        providerName: String,
        securitySchemes: List<SecurityScheme>,
        allowSources: Set<PrincipalSource>,
        private val allowPredicate: ((ServicePrincipal, ApplicationCall) -> Boolean)? = null
    ) : PrincipalAuth(providerName, securitySchemes, allowSources) {

        override fun allow(principal: MyPrincipal, call: ApplicationCall): Boolean {
            return if (principal is ServicePrincipal) {
                if (!allowSources.contains(principal.source)) return false
                allowPredicate?.invoke(principal, call) ?: true
            } else false
        }

        override fun toString(): String {
            return "ServiceAuth => AuthProvider = $id, allowSources = ${allowSources.map { it.name }}"
        }
    }

    class User(
        providerName: String,
        securitySchemes: List<SecurityScheme>,
        allowSources: Set<PrincipalSource>,
        private val typeRolesMap: Map<UserType, Set<UserRole>>,
        private val allowPredicate: ((UserPrincipal) -> Boolean)? = null,
        val runAsAuthProviderName: String? = null
    ) : PrincipalAuth(providerName, securitySchemes, allowSources) {

        override fun allow(principal: MyPrincipal, call: ApplicationCall): Boolean {
            return if (principal is UserSession) {
                if (!allowSources.contains(principal.source)) return false
                if (!typeRolesMap.containsKey(principal.userType)) return false
                val roles = typeRolesMap[principal.userType]
                if (roles.isNullOrEmpty()) return true
                if (principal.userRoles.isEmpty() || principal.userRoles.none { it in roles }) return false
                allowPredicate?.invoke(principal) ?: true
            } else false
        }

        override fun toString(): String {
            return "UserAuth => AuthProvider = $id, allowSources = ${allowSources.map { it.name }}, " +
                    "allowUserTypeRoles = ${typeRolesMap.map { it.key.name + " => " + (it.value.joinToString(",")) }}"
        }
    }
}