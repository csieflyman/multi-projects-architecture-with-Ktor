/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.auth.AuthOpenApi
import fanpoll.club.club.routes.ClubOpenApi
import fanpoll.club.notification.NotificationOpenApi
import fanpoll.club.user.routes.UserOpenApi
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.openapi.ProjectOpenApi
import fanpoll.infra.openapi.schema.component.support.DefaultSecurityScheme
import fanpoll.infra.openapi.schema.operation.definitions.PropertyDef
import fanpoll.infra.openapi.schema.operation.definitions.SchemaDataType
import fanpoll.infra.openapi.schema.operation.support.utils.ResponseUtils

object ClubProjectOpenApi {

    private val UserTypeSchema = PropertyDef(
        UserType::class.simpleName!!, SchemaDataType.string,
        kClass = UserType::class
    ).also { it.enum = ClubUserType.entries.toTypedArray().toList() }.createRef()

    private val ResponseCodeValueSchema = PropertyDef(
        "ClubResponseCode", SchemaDataType.string,
        ResponseUtils.buildResponseCodesDescription(ClubResponseCode.AllCodes),
        enum = ClubResponseCode.AllCodes.map { it.value }.toList()
    ).createRef()

    val AllSecuritySchemes = listOf(DefaultSecurityScheme.ApiKeyAuth)

    val Instance = ProjectOpenApi(ClubConst.projectId, ClubConst.urlRootPath, AllSecuritySchemes)

    init {
        Instance.components.addAll(listOf(UserTypeSchema, ResponseCodeValueSchema))

        Instance.addModuleOpenApi(ClubOpenApi)
        Instance.addModuleOpenApi(UserOpenApi)
        Instance.addModuleOpenApi(AuthOpenApi)
        Instance.addModuleOpenApi(NotificationOpenApi)
    }
}

