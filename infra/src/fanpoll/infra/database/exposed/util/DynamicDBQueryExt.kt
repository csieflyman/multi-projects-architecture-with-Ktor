/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.util

import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.PagingDataResponseDTO
import fanpoll.infra.base.response.ResponseDTO
import fanpoll.infra.database.exposed.sql.dbExecute
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.exposed.sql.Database
import kotlin.reflect.KClass

suspend inline fun <reified T : Any> DynamicQuery.queryDB(db: Database? = null): ResponseDTO = dbExecute(db) {
    if (offsetLimit != null && offsetLimit.isPaging) {
        val dbCountQuery = toDBCountQuery<T>()
        val total = dbCountQuery.count()
        val items = if (total > 0) toDBQuery<T>().toList() else listOf()
        PagingDataResponseDTO.dtoList(offsetLimit, total, items)
    } else {
        if (count == true) {
            val total = toDBCountQuery<T>().count()
            DataResponseDTO(JsonObject(mapOf("total" to JsonPrimitive(total))))
        } else {
            val data = toDBQuery<T>().toList()
            DataResponseDTO(data)
        }
    }
}

inline fun <reified T : Any> DynamicQuery.toDBQuery(): DynamicDBQuery<T> {
    return DynamicDBQuery(T::class, this, false)
}

inline fun <reified T : Any> DynamicQuery.toDBCountQuery(): DynamicDBQuery<T> {
    return DynamicDBQuery(T::class, this, true)
}

fun <T : Any> DynamicQuery.toDBQuery(objectClass: KClass<T>): DynamicDBQuery<T> {
    return DynamicDBQuery(objectClass, this, false)
}