/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.acceptLanguageItems

fun ApplicationRequest.lang(): Lang? = (cookies["lang"] ?: acceptLanguageItems().firstOrNull()?.value)?.let { Lang(it) }
// acceptLanguageItems().sortedByDescending { it.quality }.map { Lang(it.value) }

fun ApplicationCall.lang(): Lang? = attributes.getOrNull(Lang.ATTRIBUTE_KEY) ?: request.lang()