/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import com.typesafe.config.ConfigFactory
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.openapi.ProjectOpenApiManager
import io.github.config4k.extract
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

class ProjectManager(
    private val projectOpenApiManager: ProjectOpenApiManager
) {
    private val projects: MutableMap<String, Project> = mutableMapOf()

    fun register(project: Project) {
        require(!projects.containsKey(project.id))
        projects[project.id] = project

        project.principalSourceAuthConfigs.forEach {
            PrincipalSource.register(it)
        }

        UserType.register(project.userTypes)

        if (project.notificationTypes != null)
            NotificationType.register(project.notificationTypes)

        projectOpenApiManager.register(project.projectOpenApi)
    }

    companion object {

        val logger = KotlinLogging.logger {}

        inline fun <reified T> loadConfig(projectId: String): T {
            val configDir = System.getProperty("project.config.dir") ?: throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "application system property: -Dproject.config.dir is missing"
            )
            val projectConfigFile = "$configDir/application-$projectId.conf"
            try {
                logger.info { "load project config file: $projectConfigFile" }
                val myConfig = ConfigFactory.parseFile(File(projectConfigFile)).resolve()
                //logger.debug(myConfig.getConfig(projectId).entrySet().toString())
                return myConfig.extract(projectId)
            } catch (e: Throwable) {
                logger.error("fail to load project config file: $projectConfigFile", e)
                throw e
            }
        }
    }
}