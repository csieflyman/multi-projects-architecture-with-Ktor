/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support.converters

import fanpoll.infra.openapi.schema.operation.definitions.ParameterInputType
import fanpoll.infra.openapi.schema.operation.definitions.ParameterObject
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.Location
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object ParameterObjectConverter {

    @KtorExperimentalLocationsAPI
    fun toParameter(locationClass: KClass<*>): List<ParameterObject> {
        val annotation = locationClass.annotations.first { it.annotationClass == Location::class } as? Location
            ?: error("[OpenAPI]: Location Class ${locationClass.qualifiedName} @Location annotation is required")

        val pathParameters = annotation.path.split("/").filter { it.startsWith("{") && it.endsWith("}") }
            .map { it.substring(1, it.length - 1) }.toSet()

        return locationClass.primaryConstructor!!.parameters.map { kParameter ->
            val propertyName = kParameter.name!!
            val propertyDef = SchemaObjectConverter.toPropertyDef(propertyName, kParameter.type.classifier as KClass<*>)
                ?: error("location ${locationClass.qualifiedName} property $propertyName cannot map to PropertyDef")
            if (pathParameters.contains(propertyName)) {
                ParameterObject(ParameterInputType.path, true, propertyDef)
            } else {
                ParameterObject(ParameterInputType.query, !kParameter.isOptional, propertyDef)
            }
        }
    }
}