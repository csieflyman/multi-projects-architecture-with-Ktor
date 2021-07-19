/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.MyApplicationConfig
import fanpoll.infra.Project
import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.provider.UserSessionAuthValidator
import fanpoll.infra.auth.provider.service
import fanpoll.ops.features.OpsLoginService
import fanpoll.ops.features.OpsUserService
import io.ktor.application.Application
import io.ktor.auth.authentication
import io.ktor.auth.session
import io.ktor.routing.routing
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

fun Application.opsMain() {

    val appConfig = get<MyApplicationConfig>()

    val projectManager = get<ProjectManager>()
    projectManager.register(
        Project(
            OpsConst.projectId,
            appConfig.ops.auth.principalSourceAuthConfigs,
            OpsUserType.values().map { it.value },
            OpsOpenApi.Instance,
            OpsNotification.AllTypes
        )
    )

    authentication {
        service(OpsAuth.serviceAuthProviderName, appConfig.ops.auth.getServiceAuthConfigs())
        session(
            OpsAuth.userAuthProviderName,
            UserSessionAuthValidator(appConfig.ops.auth.getUserAuthConfigs(), get()).configureFunction
        )
    }

    koin {
        modules(
            module(createdAtStart = true) {
                single { OpsUserService() }
                single { OpsLoginService(get()) }
            }
        )
    }

    routing {
        ops()
    }
}