/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.util

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object ResultRowMappers {

    private val mappers: MutableMap<KClass<*>, ResultRowMapper<*>> = ConcurrentHashMap()

    fun register(vararg mappers: ResultRowMapper<*>) {
        mappers.forEach { this.mappers[it.objClass] = it }
    }

    fun <T : Any> getMapper(objClass: KClass<T>): ResultRowMapper<T>? {
        return mappers[objClass] as? ResultRowMapper<T>
    }
}