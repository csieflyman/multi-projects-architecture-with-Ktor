/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

class ProjectManager {

    private val projects: MutableMap<String, Project> = mutableMapOf()
    fun loadProjectConfig(projectId: String): Project {
        require(!projects.containsKey(projectId))
        val project = Project(projectId, loadConfig(projectId))
        projects[projectId] = project
        return project
    }

    companion object {

        private val logger = KotlinLogging.logger {}
        fun loadConfig(projectId: String): Config {
            val configDir = System.getProperty("project.config.dir") ?: throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "application system property: -Dproject.config.dir is missing"
            )
            val projectConfigFile = "$configDir/application-$projectId.conf"
            try {
                logger.info { "load project config file: $projectConfigFile" }
                val config = ConfigFactory.parseFile(File(projectConfigFile)).resolve().getConfig(projectId)
                //logger.debug { config.getConfig(projectId).entrySet().toString() }
                return config
            } catch (e: Throwable) {
                logger.error(e) { "fail to load project config file: $projectConfigFile" }
                throw e
            }
        }
    }
}