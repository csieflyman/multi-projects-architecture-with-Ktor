/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.response

import fanpoll.infra.base.exception.BaseException
import fanpoll.infra.base.i18n.lang
import fanpoll.infra.base.json.toJsonObject
import fanpoll.infra.logging.RequestAttributeKey
import io.ktor.server.application.ApplicationCall

class I18nResponseCreator(private val messagesProvider: ResponseMessagesProvider) {

    fun createErrorResponse(e: BaseException, call: ApplicationCall): ErrorResponseDTO {
        val messages = messagesProvider.preferred(call.lang())
        return ErrorResponseDTO(
            e.code,
            messages.getMessage(e), messages.getDetailMessage(e),
            call.attributes[RequestAttributeKey.ID], e.dataMap?.toJsonObject(), null
        )
    }
}