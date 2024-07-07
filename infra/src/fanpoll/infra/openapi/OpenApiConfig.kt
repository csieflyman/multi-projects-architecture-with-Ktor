/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import fanpoll.infra.config.AppInfoConfig
import fanpoll.infra.config.ServerConfig

data class OpenApiConfig(
    val info: OpenApiInfoConfig,
    val swaggerUI: SwaggerUIConfig
) {
    lateinit var appInfo: AppInfoConfig
    lateinit var server: ServerConfig
}

data class OpenApiInfoConfig(
    val description: String = ""
)

data class SwaggerUIConfig(
    val dir: String,
    val username: String?,
    val password: String?
) {

    val authEnabled: Boolean = username?.isNotBlank() == true && password?.isNotBlank() == true
}