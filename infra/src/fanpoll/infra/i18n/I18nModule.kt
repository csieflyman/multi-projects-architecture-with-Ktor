/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n

import fanpoll.infra.i18n.providers.HoconMessagesProvider
import fanpoll.infra.i18n.response.I18nResponseCreator
import fanpoll.infra.i18n.response.ResponseMessagesProvider
import io.ktor.server.application.Application
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module

fun Application.loadI18nModule(i18nConfig: I18nConfig) = loadKoinModules(module(createdAtStart = true) {
    val availableLangs = i18nConfig.availableLangs() ?: AvailableLangs(listOf(Lang.SystemDefault))
    single { availableLangs }

    initI18nResponseMessage(availableLangs)

    I18nJsonSerializers.registerSerializers()
})

private fun Module.initI18nResponseMessage(availableLangs: AvailableLangs) {
    val responseMessagesProvider = ResponseMessagesProvider(
        HoconMessagesProvider(availableLangs, "i18n/infra/response", "response_")
    )
    single { responseMessagesProvider }
    single { I18nResponseCreator(responseMessagesProvider) }
}

