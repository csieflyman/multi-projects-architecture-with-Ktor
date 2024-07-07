/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.notification

import fanpoll.ops.OpsConst
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.notificationRoutes() {
    route("${OpsConst.urlRootPath}/notification") {
        sendDynamicNotification()
    }
}