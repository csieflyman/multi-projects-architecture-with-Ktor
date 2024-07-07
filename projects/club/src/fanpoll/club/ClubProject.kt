/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.openapi.ProjectOpenApiManager
import io.github.config4k.extract
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import org.koin.ktor.ext.get

private val logger = KotlinLogging.logger {}

fun Application.clubProject(clubConfig: ClubConfig? = null) {
    logger.info { "load ${ClubConst.projectId} project..." }

    UserType.registerUserType(ClubUserType::class)
    UserRole.registerUserRole(ClubUserRole::class)

    val projectConfig = if (clubConfig != null) clubConfig
    else {
        val projectManager = get<ProjectManager>()
        val project = projectManager.loadProjectConfig(ClubConst.projectId)
        project.config.extract<ClubConfig>()
    }

    projectConfig.auth.principalSourceAuthConfigs.forEach {
        PrincipalSource.register(it)
    }

    val projectOpenApiManager = get<ProjectOpenApiManager>()
    projectOpenApiManager.register(ClubProjectOpenApi.Instance)

    loadProjectModules(projectConfig)

    loadProjectRoutes()
}