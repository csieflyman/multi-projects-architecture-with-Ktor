/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support

import fanpoll.infra.base.util.Identifiable
import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject

interface Element : Identifiable<String> {

    val name: String

    override fun getId(): String = name

    fun getDefinition(): Definition

    fun getReference(): ReferenceObject

    fun createRef(): ReferenceObject

    fun refPair(): Pair<String, ReferenceObject> = name to getReference()

    fun defPair(): Pair<String, Definition> = name to getDefinition()

    fun valuePair(): Pair<String, Element>
}

interface Parameter : Element
interface Header : Parameter
interface RequestBody : Element
interface Response : Element
interface Schema : Element
interface Example : Element