/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.MyApplicationConfig
import fanpoll.infra.auth.PrincipalSource
import fanpoll.infra.auth.PrincipalSourceAuthConfig
import fanpoll.infra.auth.UserType
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.openapi.OpenApiManager
import fanpoll.infra.openapi.support.OpenApi
import fanpoll.infra.utils.IdentifiableObject

abstract class Project(
    override val id: String,
    val principalSources: Set<PrincipalSource>,
    val userTypes: Set<UserType>,
    val openApi: OpenApi,
    val notificationTypes: List<NotificationType>? = null
) : IdentifiableObject<String>() {

    abstract fun configure(appConfig: MyApplicationConfig)

    lateinit var config: ProjectConfig

    lateinit var principalSourceAuthConfigMap: Map<PrincipalSource, PrincipalSourceAuthConfig<*>>
}

interface ProjectConfig {

    val auth: ProjectAuthConfig
}

interface ProjectAuthConfig {

    fun getPrincipalSourceAuthConfigs(): List<PrincipalSourceAuthConfig<*>>
}

object ProjectManager {

    private val projects: MutableMap<String, Project> = mutableMapOf()
    private lateinit var appConfig: MyApplicationConfig

    fun init(appConfig: MyApplicationConfig) {
        ProjectManager.appConfig = appConfig
    }

    fun register(project: Project) {
        require(!projects.containsKey(project.id))
        projects[project.id] = project

        project.configure(appConfig)

        registerPrincipalSources(project)
        UserType.register(project.userTypes)
        if (project.notificationTypes != null)
            NotificationType.register(project.notificationTypes)

        OpenApiManager.registerProject(project.openApi)
    }

    private fun registerPrincipalSources(project: Project) {
        val principalSourceAuthConfigs = project.config.auth.getPrincipalSourceAuthConfigs()
        project.principalSourceAuthConfigMap = project.principalSources.map { source ->
            source to (principalSourceAuthConfigs.find { it.id == source.id }
                ?: error("principalSource ${source.id} config is missing"))
        }.toMap()

        PrincipalSource.register(project.principalSourceAuthConfigMap)
    }
}