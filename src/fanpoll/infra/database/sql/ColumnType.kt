/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.sql

import fanpoll.infra.base.json.CustomNameEnum
import fanpoll.infra.base.json.CustomNameEnumConverter
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.vendors.currentDialect
import kotlin.reflect.KClass

fun <E : Enum<E>> Table.enumerationByName(
    name: String, length: Int, klass: KClass<E>,
    converter: CustomNameEnumConverter
): Column<CustomNameEnum<E>> = registerColumn(name, object : VarCharColumnType(length) {
    override fun valueFromDB(value: Any): CustomNameEnum<E> = when (value) {
        is String -> CustomNameEnum(value, klass, converter)
        is Enum<*> -> CustomNameEnum(value as E, converter)
        else -> error("$value of ${value::class.qualifiedName} is not valid for enum ${klass.qualifiedName}")
    }

    override fun notNullValueToDB(value: Any): Any = converter.toCustomName
})


fun Table.json(name: String): Column<JsonElement> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): JsonElement = fanpoll.infra.base.json.json.parseToJsonElement(value as String)
    override fun notNullValueToDB(value: Any): Any = value.toString()
})

fun Table.jsonObject(name: String): Column<JsonObject> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): JsonElement =
        fanpoll.infra.base.json.json.parseToJsonElement(value as String).jsonObject

    override fun notNullValueToDB(value: Any): Any = value.toString()
})

fun Table.jsonArray(name: String): Column<JsonArray> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): JsonElement =
        fanpoll.infra.base.json.json.parseToJsonElement(value as String).jsonArray

    override fun notNullValueToDB(value: Any): Any = value.toString()
})

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> Table.dto(name: String): Column<T> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): T =
        fanpoll.infra.base.json.json.decodeFromString(T::class.serializer(), value as String)

    override fun notNullValueToDB(value: Any): Any =
        fanpoll.infra.base.json.json.encodeToString(T::class.serializer(), value as T)
})

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> Table.dtoList(name: String): Column<List<T>> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): List<T> =
        fanpoll.infra.base.json.json.decodeFromString(ListSerializer(T::class.serializer()), value as String)

    override fun notNullValueToDB(value: Any): Any =
        fanpoll.infra.base.json.json.encodeToString(ListSerializer(T::class.serializer()), value as List<T>)
})