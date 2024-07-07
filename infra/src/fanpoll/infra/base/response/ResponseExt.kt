/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.base.response

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend fun ApplicationCall.respond(responseDTO: ResponseDTO) {
    this.respond(responseDTO.code.httpStatusCode, responseDTO)
}