/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.monitor

import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag

object MonitorOpenApi {

    private val MonitorTag = Tag("monitor")

    val HealthCheck = RouteApiOperation("HealthCheck", listOf(MonitorTag))
}