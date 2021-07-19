/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import fanpoll.infra.base.json.Jackson
import fanpoll.infra.base.json.json
import fanpoll.infra.openapi.schema.operation.support.Definition
import fanpoll.infra.openapi.schema.operation.support.Example
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.serializer

class ExampleObject(
    name: String,
    val value: Any,
    val summary: String,
    val description: String? = null
    //val externalValue: String?
) : Definition(name), Example {

    override fun componentsFieldName(): String = "examples"

    override fun defPair(): Pair<String, ExampleObject> = name to this

    override fun valuePair(): Pair<String, Example> = if (hasRef()) refPair() else defPair()

    companion object {

        @OptIn(InternalSerializationApi::class)
        inline operator fun <reified T : Any> invoke(
            name: String, summary: String, description: String? = null, obj: T
        ): ExampleObject {
            val jsonObject = json.encodeToJsonElement(T::class.serializer(), obj)
            return ExampleObject(name, Jackson.parse(jsonObject.toString()), summary, description)
        }

        @OptIn(InternalSerializationApi::class)
        inline operator fun <reified T : Any> invoke(
            name: String, summary: String, description: String? = null, objs: List<T>
        ): ExampleObject {
            val jsonArray = JsonArray(objs.map { json.encodeToJsonElement(T::class.serializer(), it) })
            return ExampleObject(name, Jackson.parse(jsonArray.toString()), summary, description)
        }
    }
}