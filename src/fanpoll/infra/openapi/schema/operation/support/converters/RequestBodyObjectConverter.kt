/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support.converters

import fanpoll.infra.openapi.schema.component.definitions.ComponentsObject
import fanpoll.infra.openapi.schema.operation.definitions.MediaTypeObject
import fanpoll.infra.openapi.schema.operation.definitions.RequestBodyObject
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils
import io.ktor.http.ContentType
import kotlin.reflect.KType

object RequestBodyObjectConverter {

    fun toRequestBody(components: ComponentsObject, modelKType: KType): RequestBodyObject {
        val modelName = DataModelUtils.getSchemaName(modelKType)
        return RequestBodyObject(
            "$modelName-RequestBody",
            mapOf(
                ContentType.Application.Json to
                        MediaTypeObject(schema = SchemaObjectConverter.toSchema(components, modelKType, modelName))
            )
        )
    }
}