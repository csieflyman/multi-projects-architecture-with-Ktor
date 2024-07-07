/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.log

import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag

object LogOpenApi {

    private val queryLogTag = Tag("queryLog")

    val QueryRequestLog = RouteApiOperation("QueryRequestLog", listOf(queryLogTag))
    val QueryErrorLog = RouteApiOperation("QueryErrorLog", listOf(queryLogTag))
    val QueryLoginLog = RouteApiOperation("QueryLoginLog", listOf(queryLogTag))
    val QueryNotificationMessageLog = RouteApiOperation("QueryNotificationMessageLog", listOf(queryLogTag))
}