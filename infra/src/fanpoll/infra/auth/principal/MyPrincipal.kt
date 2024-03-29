/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import fanpoll.infra.base.util.IdentifiableObject
import io.ktor.server.auth.Principal

abstract class MyPrincipal : Principal, IdentifiableObject<String>() {

    abstract val source: PrincipalSource
}