/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.auth.provider.PrincipalSourceAuthConfig
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.openapi.ProjectOpenApi

class Project(
    override val id: String,
    val principalSourceAuthConfigs: List<PrincipalSourceAuthConfig>,
    val userTypes: List<UserType>,
    val projectOpenApi: ProjectOpenApi,
    val notificationTypes: List<NotificationType>? = null
) : IdentifiableObject<String>()