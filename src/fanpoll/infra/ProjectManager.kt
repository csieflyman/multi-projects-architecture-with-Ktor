/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.openapi.ProjectOpenApiManager

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
}