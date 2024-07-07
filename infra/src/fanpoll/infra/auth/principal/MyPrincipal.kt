/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import io.ktor.server.auth.Principal

interface MyPrincipal : Principal {
    val id: String
    val source: PrincipalSource
}