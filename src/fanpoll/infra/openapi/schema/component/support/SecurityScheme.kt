/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.component.support

import fanpoll.infra.openapi.schema.component.definitions.SecuritySchemeObject
import fanpoll.infra.openapi.schema.operation.definitions.SecurityRequirementObject

interface SecurityScheme {

    val name: String
    val value: SecuritySchemeObject

    companion object {

        fun apiKeyAuth(schemeName: String, headerName: String) = object : SecurityScheme {
            override val name: String = schemeName
            override val value: SecuritySchemeObject = SecuritySchemeObject(type = "apiKey", `in` = "header", name = headerName)
        }
    }

    fun createSecurity(scopes: List<String> = listOf()) = SecurityRequirementObject(this, scopes)
}

enum class DefaultSecurityScheme(override val value: SecuritySchemeObject) : SecurityScheme {
    ApiKeyAuth(SecuritySchemeObject(type = "apiKey", `in` = "header", name = "X-API-Key")),
    BasicAuth(SecuritySchemeObject(type = "http", scheme = "basic")),
    BearerAuth(SecuritySchemeObject(type = "http", scheme = "bearer")),
    OpenID(SecuritySchemeObject(type = "openIdConnect")),
    //OAuth2(SecuritySchemeObject(type = "oauth2"))
}