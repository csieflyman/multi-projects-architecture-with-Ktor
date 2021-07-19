/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

class ServicePrincipal(
    override val source: PrincipalSource
) : MyPrincipal() {

    override val id: String = source.id

    override fun toString() = id
}