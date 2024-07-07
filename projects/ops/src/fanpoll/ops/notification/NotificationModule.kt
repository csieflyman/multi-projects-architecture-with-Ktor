/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.notification

import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.i18n.providers.PropertiesMessagesProvider
import fanpoll.infra.notification.message.i18n.I18nNotificationMessagesProvider
import fanpoll.infra.notification.message.i18n.I18nProjectNotificationMessagesProvider
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.database.exposed.OpsDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get

private val logger = KotlinLogging.logger {}

fun Application.loadNotificationModule() {

    initI18nNotificationMessage(this)

    OpsKoinContext.koin.loadModules(listOf(
        module(createdAtStart = true) {
            val opsDatabase = OpsKoinContext.koin.get<Database>(named(OpsDatabase.Ops.name))
            single { DynamicNotificationDataDBLoader(opsDatabase) }
        }
    ))
}

private fun initI18nNotificationMessage(application: Application) {
    val availableLangs = application.get<AvailableLangs>()
    val i18NProjectNotificationMessagesProvider = application.get<I18nProjectNotificationMessagesProvider>()
    i18NProjectNotificationMessagesProvider.addProvider(
        OpsConst.projectId,
        I18nNotificationMessagesProvider(
            PropertiesMessagesProvider(
                availableLangs,
                "i18n/${OpsConst.projectId}/notification",
                "notification_"
            )
        )
    )
}