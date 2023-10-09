/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.base.i18n.AvailableLangs
import fanpoll.infra.base.i18n.HoconMessagesProvider
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.response.I18nResponseCreator
import fanpoll.infra.base.response.ResponseMessagesProvider
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.RequestScope

fun KoinApplication.koinBaseModule(appConfig: MyApplicationConfig): Module {

    val availableLangs = appConfig.infra.i18n?.availableLangs() ?: AvailableLangs(listOf(Lang.SystemDefault))
    val responseMessagesProvider = ResponseMessagesProvider(
        HoconMessagesProvider(availableLangs, "i18n/response", "response_")
    )

    return module(createdAtStart = true) {
        //  i18n
        single { availableLangs }
        single { responseMessagesProvider }
        single { I18nResponseCreator(responseMessagesProvider) }

        scope<RequestScope> {}
    }
}