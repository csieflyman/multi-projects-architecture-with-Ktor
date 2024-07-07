/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.openapi.ProjectOpenApi
import fanpoll.infra.openapi.schema.component.support.DefaultSecurityScheme
import fanpoll.infra.openapi.schema.operation.definitions.PropertyDef
import fanpoll.infra.openapi.schema.operation.definitions.SchemaDataType
import fanpoll.infra.openapi.schema.operation.support.utils.ResponseUtils
import fanpoll.ops.auth.AuthOpenApi
import fanpoll.ops.log.LogOpenApi
import fanpoll.ops.monitor.MonitorOpenApi
import fanpoll.ops.notification.NotificationOpenApi
import fanpoll.ops.release.app.routes.AppReleaseOpenApi
import fanpoll.ops.report.ReportOpenApi
import fanpoll.ops.user.routes.UserOpenApi

object OpsProjectOpenApi {

    private val UserTypeSchema = PropertyDef(
        UserType::class.simpleName!!, SchemaDataType.string,
        kClass = UserType::class
    ).also { it.enum = OpsUserType.entries.toTypedArray().toList() }.createRef()

    private val ResponseCodeValueSchema = PropertyDef(
        "OpsResponseCode", SchemaDataType.string,
        ResponseUtils.buildResponseCodesDescription(OpsResponseCodes.AllCodes),
        enum = OpsResponseCodes.AllCodes.map { it.value }.toList()
    ).createRef()

    val AllSecuritySchemes = listOf(DefaultSecurityScheme.ApiKeyAuth)

    val Instance = ProjectOpenApi(OpsConst.projectId, OpsConst.urlRootPath, AllSecuritySchemes)

    init {
        Instance.components.addAll(listOf(UserTypeSchema, ResponseCodeValueSchema))

        Instance.addModuleOpenApi(AuthOpenApi)
        Instance.addModuleOpenApi(MonitorOpenApi)
        Instance.addModuleOpenApi(ReportOpenApi)
        Instance.addModuleOpenApi(UserOpenApi)
        Instance.addModuleOpenApi(LogOpenApi)
        Instance.addModuleOpenApi(AppReleaseOpenApi)
        Instance.addModuleOpenApi(NotificationOpenApi)
    }
}
