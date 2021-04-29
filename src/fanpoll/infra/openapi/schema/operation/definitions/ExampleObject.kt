/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import fanpoll.infra.openapi.schema.operation.support.Definition
import fanpoll.infra.openapi.schema.operation.support.Example

class ExampleObject(
    name: String,
    val value: Any,
    val summary: String,
    val description: String? = null,
    //val externalValue: String?
) : Definition(name), Example {

    override fun componentsFieldName(): String = "examples"

    override fun defPair(): Pair<String, ExampleObject> = name to this

    override fun valuePair(): Pair<String, Example> = if (hasRef()) refPair() else defPair()
}