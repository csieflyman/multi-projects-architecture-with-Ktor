/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.definition

class Root(
    val openapi: String = "3.0.3",
    val info: Information,
    val servers: List<Server>,
    val tags: Set<Tag>,
    //@JsonIgnore val security: List<Security>
) {
    lateinit var components: Components
        private set
    val paths: Paths = Paths()

    fun initComponents(securitySchemes: List<SecurityScheme>) {
        components = Components(securitySchemes)
    }
}

class Information(
    val title: String,
    val description: String,
    val version: String,
    val contact: Contact? = null
)

class Contact(
    val name: String? = null,
    val url: String? = null,
    val email: String? = null
)

class Server(
    val url: String,
    val description: String? = null
)

data class Tag(
    val name: String,
    val description: String? = null,
    val externalDocs: String? = null
)

class Security(val scheme: SecurityScheme, val scopes: List<String> = listOf())

interface SecurityScheme {

    val name: String
    val value: SecuritySchemeObject

    companion object {

        fun apiKeyAuth(schemeName: String, headerName: String) = object : SecurityScheme {
            override val name: String = schemeName
            override val value: SecuritySchemeObject = SecuritySchemeObject(type = "apiKey", `in` = "header", name = headerName)
        }
    }

    fun createSecurity(scopes: List<String> = listOf()) = Security(this, scopes)
}

enum class DefaultSecurityScheme(override val value: SecuritySchemeObject) : SecurityScheme {
    ApiKeyAuth(SecuritySchemeObject(type = "apiKey", `in` = "header", name = "X-API-Key")),
    BasicAuth(SecuritySchemeObject(type = "http", scheme = "basic")),
    BearerAuth(SecuritySchemeObject(type = "http", scheme = "bearer")),
    OpenID(SecuritySchemeObject(type = "openIdConnect")),
    //OAuth2(SecuritySchemeObject(type = "oauth2"))
}


