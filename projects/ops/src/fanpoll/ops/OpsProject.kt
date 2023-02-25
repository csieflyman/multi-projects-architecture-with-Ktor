/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.Project
import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.provider.UserSessionAuthValidator
import fanpoll.infra.auth.provider.service
import fanpoll.infra.base.i18n.AvailableLangs
import fanpoll.infra.base.i18n.HoconMessagesProvider
import fanpoll.infra.base.i18n.PropertiesMessagesProvider
import fanpoll.infra.base.response.ResponseMessagesProvider
import fanpoll.infra.notification.i18n.I18nNotificationMessagesProvider
import fanpoll.infra.notification.i18n.I18nNotificationProjectMessages
import fanpoll.ops.features.OpsLoginService
import fanpoll.ops.features.OpsUserService
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.session
import io.ktor.server.routing.routing
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.koin

private val logger = KotlinLogging.logger {}

fun Application.opsMain() {
    logger.info { "load ${OpsConst.projectId} project..." }
    val application = this

    val projectManager = get<ProjectManager>()
    val projectConfig = ProjectManager.loadConfig<OpsConfig>(OpsConst.projectId)
    projectManager.register(
        Project(
            OpsConst.projectId,
            projectConfig.auth.principalSourceAuthConfigs,
            OpsUserType.values().map { it.value },
            OpsOpenApi.Instance,
            OpsNotification.AllTypes
        )
    )

    authentication {
        service(OpsAuth.serviceAuthProviderName, projectConfig.auth.getServiceAuthConfigs())
        session(
            OpsAuth.userAuthProviderName,
            UserSessionAuthValidator(projectConfig.auth.getUserAuthConfigs(), application.get()).configureFunction
        )
    }

    val availableLangs = get<AvailableLangs>()

    val responseMessagesProvider = get<ResponseMessagesProvider>()
    responseMessagesProvider.merge(
        HoconMessagesProvider(availableLangs, "i18n/response/${OpsConst.projectId}", "response_")
    )

    val i18nNotificationProjectMessages = get<I18nNotificationProjectMessages>()
    i18nNotificationProjectMessages.addProvider(
        OpsConst.projectId,
        I18nNotificationMessagesProvider(
            PropertiesMessagesProvider(
                availableLangs,
                "i18n/notification/${OpsConst.projectId}",
                "notification_"
            )
        )
    )

    koin {
        modules(
            module(createdAtStart = true) {
                single { projectConfig }
                single { OpsUserService() }
                single { OpsLoginService(get()) }
            }
        )
    }

    routing {
        ops()
    }
}