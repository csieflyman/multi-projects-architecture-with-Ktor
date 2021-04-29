/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.MyApplicationConfig
import fanpoll.infra.Project
import fanpoll.infra.ProjectConfig
import fanpoll.infra.auth.PrincipalSource
import fanpoll.infra.openapi.ProjectOpenApi

object OpsConst {

    const val projectId = "ops"

    const val urlRootPath = "/ops"
}

private val openApi = ProjectOpenApi(
    OpsConst.projectId, OpsConst.urlRootPath, OpsAuth.allAuthSchemes, OpsOpenApiOperations.all()
)

val OpsProject = object : Project(
    OpsConst.projectId,
    OpsPrincipalSources.All, emptySet(),
    openApi
) {

    override fun configure(appConfig: MyApplicationConfig) {
        config = appConfig.ops
    }
}

data class OpsConfig(
    override val auth: OpsAuthConfig
) : ProjectConfig

object OpsPrincipalSources {

    val Root = PrincipalSource(OpsConst.projectId, "root", false)
    val OperationsTeam = PrincipalSource(OpsConst.projectId, "operationsTeam", false)
    val AppTeam = PrincipalSource(OpsConst.projectId, "appTeam", false)

    val All: Set<PrincipalSource> = setOf(Root, OperationsTeam, AppTeam)
}