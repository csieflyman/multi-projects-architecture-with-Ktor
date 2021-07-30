/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.component.definitions

class SecuritySchemeObject(
    val type: String, val `in`: String? = null, val name: String? = null,
    val scheme: String? = null, var openIdConnectUrl: String? = null,
    //var flows:
)