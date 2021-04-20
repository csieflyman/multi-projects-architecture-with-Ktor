/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi

import com.neovisionaries.i18n.LanguageCode

data class OpenApiConfig(
    val info: OpenApiInfoConfig,
    val swaggerUI: SwaggerUIConfig
) {
    companion object {
        val langCode = LanguageCode.zh
    }
}

data class OpenApiInfoConfig(
    val env: String,
    val gitTagVersion: String,
    val gitCommitVersion: String,
    val buildTime: String,
    val description: String = ""
)

data class SwaggerUIConfig(
    val dir: String?,
    val username: String?,
    val password: String?
) {
    companion object {
        const val authProviderName: String = "swaggerUI"
    }

    val needAuth: Boolean = username != null && password != null
}