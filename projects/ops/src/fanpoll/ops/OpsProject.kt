/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

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

fun Application.opsProject(opsConfig: OpsConfig? = null) {
    logger.info { "load ${OpsConst.projectId} project..." }

    UserType.registerUserType(OpsUserType::class)
    UserRole.registerUserRole(OpsUserRole::class)

    val projectConfig = if (opsConfig != null) opsConfig
    else {
        val projectManager = get<ProjectManager>()
        val project = projectManager.loadProjectConfig(OpsConst.projectId)
        project.config.extract<OpsConfig>()
    }

    projectConfig.auth.principalSourceAuthConfigs.forEach {
        PrincipalSource.register(it)
    }

    val projectOpenApiManager = get<ProjectOpenApiManager>()
    projectOpenApiManager.register(OpsProjectOpenApi.Instance)

    loadProjectModules(projectConfig)

    loadProjectRoutes()
}