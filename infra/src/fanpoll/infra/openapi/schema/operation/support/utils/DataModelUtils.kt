/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support.utils

import com.fasterxml.jackson.databind.node.ArrayNode
import kotlinx.serialization.json.JsonArray
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

object DataModelUtils {

    val UnitKType = typeOf<Unit>()
    val AnyKType = typeOf<Any>()
    val LongIdKType = typeOf<Long>()
    val StringIdKType = typeOf<String>()
    val UUIDIdKType = typeOf<UUID>()

    val CollectionKType = typeOf<Collection<*>?>()
    val KotlinxJsonArrayKType = typeOf<JsonArray?>()
    val JacksonJsonArrayKType = typeOf<ArrayNode?>()

    fun getSchemaName(modelKType: KType): String = if (modelKType.isSubtypeOf(CollectionKType))
        (modelKType.arguments[0].type!!.classifier as KClass<*>).simpleName + "Array"
    else (modelKType.classifier as KClass<*>).simpleName!!
}