/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.ProjectAuthConfig
import fanpoll.infra.auth.*
import fanpoll.infra.openapi.ProjectOpenApi
import io.ktor.auth.Authentication

object OpsAuth {

    const val serviceProviderName = "${OpsConst.projectId}-service"

    private val serviceAuthSchemes = listOf(ProjectOpenApi.apiKeySecurityScheme)

    val allAuthSchemes = listOf(ProjectOpenApi.apiKeySecurityScheme)

    val Root = PrincipalAuth.Service.private(serviceProviderName, serviceAuthSchemes, setOf(OpsPrincipalSources.Root))

    val OperationsTeam = PrincipalAuth.Service.private(serviceProviderName, serviceAuthSchemes, setOf(OpsPrincipalSources.OperationsTeam))

    val AppTeam = PrincipalAuth.Service.private(serviceProviderName, serviceAuthSchemes, setOf(OpsPrincipalSources.AppTeam))

    val Dev = PrincipalAuth.Service.private(
        serviceProviderName, serviceAuthSchemes,
        setOf(OpsPrincipalSources.Root, OpsPrincipalSources.OperationsTeam, OpsPrincipalSources.AppTeam)
    )
}

data class OpsAuthConfig(
    private val root: ServiceAuthConfig,
    private val operationsTeam: ServiceAuthConfig,
    private val appTeam: ServiceAuthConfig
) : ProjectAuthConfig {

    override fun getPrincipalSourceAuthConfigs(): List<PrincipalSourceAuthConfig<ServiceAuthApiKeyCredential>> =
        listOf(root, operationsTeam, appTeam)
}

fun Authentication.Configuration.ops(authConfig: OpsAuthConfig) {

    serviceApiKey(OpsAuth.serviceProviderName, authConfig.getPrincipalSourceAuthConfigs())
}