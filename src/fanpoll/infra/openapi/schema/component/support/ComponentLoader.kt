/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.openapi.schema.component.support

import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject

interface ComponentLoader {

    fun load(): List<ReferenceObject>
}

