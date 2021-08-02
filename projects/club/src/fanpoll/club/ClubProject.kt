/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.user.ClubLoginService
import fanpoll.club.user.ClubUserService
import fanpoll.infra.Project
import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.provider.UserSessionAuthValidator
import fanpoll.infra.auth.provider.runAs
import fanpoll.infra.auth.provider.service
import fanpoll.infra.base.i18n.AvailableLangs
import fanpoll.infra.base.i18n.HoconMessagesProvider
import fanpoll.infra.base.i18n.PropertiesMessagesProvider
import fanpoll.infra.base.response.ResponseMessagesProvider
import fanpoll.infra.notification.i18n.I18nNotificationMessagesProvider
import fanpoll.infra.notification.i18n.I18nNotificationProjectMessages
import io.ktor.application.Application
import io.ktor.auth.authentication
import io.ktor.auth.session
import io.ktor.routing.routing
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

private val logger = KotlinLogging.logger {}

fun Application.clubMain() {
    logger.info { "load ${ClubConst.projectId} project..." }

    val projectManager = get<ProjectManager>()
    val projectConfig = ProjectManager.loadConfig<ClubConfig>(ClubConst.projectId)
    projectManager.register(
        Project(
            ClubConst.projectId,
            projectConfig.auth.principalSourceAuthConfigs,
            ClubUserType.values().map { it.value },
            ClubOpenApi.Instance,
            ClubNotification.AllTypes
        )
    )

    authentication {
        service(ClubAuth.serviceAuthProviderName, projectConfig.auth.getServiceAuthConfigs())
        session(
            ClubAuth.userAuthProviderName,
            UserSessionAuthValidator(projectConfig.auth.getUserAuthConfigs(), get()).configureFunction
        )
        runAs(ClubAuth.userRunAsAuthProviderName, projectConfig.auth.getRunAsConfigs())
    }

    val availableLangs = get<AvailableLangs>()

    val responseMessagesProvider = get<ResponseMessagesProvider>()
    responseMessagesProvider.merge(
        HoconMessagesProvider(availableLangs, "i18n/response/${ClubConst.projectId}", "response_")
    )

    val i18nNotificationProjectMessages = get<I18nNotificationProjectMessages>()
    i18nNotificationProjectMessages.addProvider(
        ClubConst.projectId,
        I18nNotificationMessagesProvider(
            PropertiesMessagesProvider(
                availableLangs,
                "i18n/notification/${ClubConst.projectId}",
                "notification_"
            )
        )
    )

    koin {
        modules(
            module(createdAtStart = true) {
                single { projectConfig }
                single { ClubUserService() }
                single { ClubLoginService(get()) }
            }
        )
    }

    routing {
        club()
    }
}