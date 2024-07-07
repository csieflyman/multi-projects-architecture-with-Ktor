/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops.monitor

import fanpoll.infra.auth.authorize
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.openapi.route.get
import fanpoll.ops.OpsAuth
import io.ktor.server.application.call
import io.ktor.server.routing.Route

fun Route.healthCheckRoute() {

    authorize(OpsAuth.Monitor) {

        get<Unit>("/healthCheck", MonitorOpenApi.HealthCheck) {
            call.respond(CodeResponseDTO.OK)
        }
    }

}