/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.i18n.providers.HoconMessagesProvider
import fanpoll.infra.i18n.response.ResponseMessagesProvider
import io.ktor.server.application.Application
import org.koin.ktor.ext.get

fun Application.loadBaseModule() {
    initResponsesMessageProvider()
}

private fun Application.initResponsesMessageProvider() {
    val availableLangs = get<AvailableLangs>()
    val responseMessagesProvider = get<ResponseMessagesProvider>()
    responseMessagesProvider.merge(
        HoconMessagesProvider(availableLangs, "i18n/${ClubConst.projectId}/response", "response_")
    )
}