/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.utils

import kotlinx.serialization.json.JsonElement
import org.kpropmap.applyProps
import org.kpropmap.deserialize
import org.kpropmap.propMapOf
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

fun Any.toNotNullMap(prefix: String? = null): Map<String, Any> {
    val map = propMapOf(this).filterNotNull()
    return if (prefix != null) map.mapKeys { "$prefix.$it" } else map
}

fun Any.toNullableMap(prefix: String? = null): Map<String, Any?> {
    val map = propMapOf(this)
    return if (prefix != null) map.mapKeys { "$prefix.$it" } else map
}

inline fun <reified T : Any> Map<String, Any?>.toObject(): T {
    return propMapOf(this).deserialize()
}

inline fun <reified T : Any> Map<String, Any?>.mergeTo(obj: T, exclude: List<KProperty1<T, *>>?): T {
    return propMapOf(this).applyProps(obj, exclude)
}

fun <T : Any, R : Any> T.copyPropsFrom(fromObject: R, ignoreNullValue: Boolean? = true, vararg excludes: KProperty<*>) {
    val mutableProps =
        this::class.memberProperties.filterIsInstance<KMutableProperty1<T, *>>().filterNot { excludes.contains(it) }
    val sourceProps = fromObject::class.memberProperties
    mutableProps.forEach { targetProp ->
        sourceProps.find {
            // make sure properties have same name and compatible types
            it.name == targetProp.name && targetProp.returnType.isSupertypeOf(it.returnType)
        }?.let { matchingProp ->
            val value = matchingProp.getter.call(fromObject)
            if (ignoreNullValue == false || value != null)
                targetProp.setter.call(this, matchingProp.getter.call(fromObject))
        }
    }
}

private val typeOfJsonElement = typeOf<JsonElement>()

fun <T : Any> T.copyJsonPropsFrom(from: Any) {
    copyPropsFrom(
        from, true,
        *javaClass.kotlin.memberProperties.filterNot {
            typeOfJsonElement.isSupertypeOf(it.returnType)
        }.toTypedArray()
    )
}