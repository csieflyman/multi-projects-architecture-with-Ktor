/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.notification

import fanpoll.club.ClubConst
import fanpoll.club.ClubKoinContext
import fanpoll.club.database.exposed.ClubDatabase
import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.i18n.providers.PropertiesMessagesProvider
import fanpoll.infra.notification.message.i18n.I18nNotificationMessagesProvider
import fanpoll.infra.notification.message.i18n.I18nProjectNotificationMessagesProvider
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin

fun Application.loadNotificationModule() {

    val availableLangs = get<AvailableLangs>()
    val i18NProjectNotificationMessagesProvider = get<I18nProjectNotificationMessagesProvider>()
    i18NProjectNotificationMessagesProvider.addProvider(
        ClubConst.projectId,
        I18nNotificationMessagesProvider(
            PropertiesMessagesProvider(
                availableLangs,
                "i18n/${ClubConst.projectId}/notification",
                "notification_"
            )
        )
    )

    val infraKoin = getKoin()
    ClubKoinContext.koin.loadModules(listOf(
        module(createdAtStart = true) {
            val clubDatabase = ClubKoinContext.koin.get<Database>(named(ClubDatabase.Club.name))
            single { DynamicNotificationDataDBLoader(clubDatabase, infraKoin.get()) }
        }
    ))
}