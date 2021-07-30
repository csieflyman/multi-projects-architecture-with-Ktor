/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import fanpoll.infra.openapi.schema.component.support.SecurityScheme

class SecurityRequirementObject(val scheme: SecurityScheme, val scopes: List<String> = listOf())