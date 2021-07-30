/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import fanpoll.infra.openapi.schema.operation.support.Example
import fanpoll.infra.openapi.schema.operation.support.Schema

class MediaTypeObject(
    val schema: Schema,
    var example: Any? = null,
    var examples: MutableMap<String, Example>? = null
)