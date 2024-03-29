/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.ops.features.*
import io.ktor.server.routing.Routing

fun Routing.ops() {

    opsUser()
    opsLogin()

    opsMonitor()
    opsDataReport()
    opsAppRelease()
    opsQueryLog()
}