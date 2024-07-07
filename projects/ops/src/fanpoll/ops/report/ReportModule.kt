/*
 * Copyright (c) 2023. fanpoll All rights reserved.
 */

package fanpoll.ops.report

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.i18n.providers.PropertiesMessagesProvider
import fanpoll.infra.report.data.loaders.DynamicDBQueryReportDataLoader
import fanpoll.infra.report.data.loaders.ReportDataLoader
import fanpoll.infra.report.i18n.I18nProjectReportMessagesProvider
import fanpoll.infra.report.i18n.I18nReportMessagesProvider
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.database.exposed.OpsDatabase
import fanpoll.ops.user.dtos.UserDTO
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

fun Application.loadReportModule() {

    val availableLangs = get<AvailableLangs>()
    val i18nProjectReportMessagesProvider = get<I18nProjectReportMessagesProvider>()
    i18nProjectReportMessagesProvider.addProvider(
        OpsConst.projectId,
        I18nReportMessagesProvider(
            PropertiesMessagesProvider(
                availableLangs,
                "i18n/${OpsConst.projectId}/report",
                "report_"
            )
        )
    )

    OpsKoinContext.koin.loadModules(listOf(
        module(createdAtStart = true) {
            val opsDatabase = OpsKoinContext.koin.get<Database>(named(OpsDatabase.Ops.name))
            single<ReportDataLoader>(named(OpsReportType.UserDynamicQuery.id)) {
                DynamicDBQueryReportDataLoader(typeOf<UserDTO>().classifier as KClass<EntityDTO<*>>, opsDatabase)
            }
        }
    ))
}