/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support

import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject
import fanpoll.infra.utils.Identifiable

interface Element : Identifiable<String> {

    val name: String

    override fun getId(): String = name

    fun getDefinition(): Definition

    fun getReference(): ReferenceObject
}

interface Parameter : Element
interface Header : Parameter
interface RequestBody : Element
interface Response : Element
interface Schema : Element
interface Example : Element