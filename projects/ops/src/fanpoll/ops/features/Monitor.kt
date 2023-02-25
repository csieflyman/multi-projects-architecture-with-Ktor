/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops.features

import fanpoll.infra.auth.authorize
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.openapi.get
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsOpenApi
import io.ktor.server.application.call
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.opsMonitor() {

    route("${OpsConst.urlRootPath}/monitor") {

        authorize(OpsAuth.Monitor) {

            get<Unit>("/healthCheck", OpsOpenApi.HealthCheck) {
                call.respond(CodeResponseDTO.OK)
            }
        }
    }

}