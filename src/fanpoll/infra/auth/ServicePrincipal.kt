/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

class ServicePrincipal(
    override val source: PrincipalSource,
    val role: ServiceRole
) : MyPrincipal() {

    override val id: String = source.id

    override fun toString() = id
}

enum class ServiceRole : PrincipalRole {

    Private, Public;

    override val id: String = name

    override fun toString() = id
}