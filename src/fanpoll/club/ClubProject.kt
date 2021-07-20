/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.MyApplicationConfig
import fanpoll.club.user.ClubLoginService
import fanpoll.club.user.ClubUserService
import fanpoll.infra.Project
import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.provider.UserSessionAuthValidator
import fanpoll.infra.auth.provider.runAs
import fanpoll.infra.auth.provider.service
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

    val appConfig = get<MyApplicationConfig>()
    val projectManager = get<ProjectManager>()

    projectManager.register(
        Project(
            ClubConst.projectId,
            appConfig.club.auth.principalSourceAuthConfigs,
            ClubUserType.values().map { it.value },
            ClubOpenApi.Instance,
            ClubNotification.AllTypes
        )
    )

    authentication {
        service(ClubAuth.serviceAuthProviderName, appConfig.club.auth.getServiceAuthConfigs())
        session(
            ClubAuth.userAuthProviderName,
            UserSessionAuthValidator(appConfig.club.auth.getUserAuthConfigs(), get()).configureFunction
        )
        runAs(ClubAuth.userRunAsAuthProviderName, appConfig.club.auth.getRunAsConfigs())
    }

    koin {
        modules(
            module(createdAtStart = true) {
                single { ClubUserService() }
                single { ClubLoginService(get()) }
            }
        )
    }

    routing {
        club()
    }
}