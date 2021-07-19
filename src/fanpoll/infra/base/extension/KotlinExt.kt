/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.base.extension

import org.kpropmap.applyProps
import org.kpropmap.deserialize
import org.kpropmap.propMapOf
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.memberProperties

// ========== Map & Bean ==========
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

fun <K : Any, V : Any> Map<out K?, V?>.filterNotNull(): Map<K, V> {
    return filterValues { it != null } as Map<K, V>
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

// ========== Iterable ==========

// example: nonMatchFunc could be logging
inline fun <T> Iterable<T>.filterAndApplyNonMatchFuncForEach(
    nonMatchFunc: (T) -> Unit,
    predicate: (T) -> Boolean
): List<T> {
    val destination = ArrayList<T>()
    for (element in this) if (predicate(element)) destination.add(element) else nonMatchFunc(element)
    return destination
}

// example nonMatchFunc could be logging
inline fun <T> Iterable<T>.filterAndApplyNonMatchFunc(
    nonMatchFunc: (ArrayList<T>) -> Unit,
    predicate: (T) -> Boolean
): List<T> {
    val nonMatchElements = ArrayList<T>()
    val destination = ArrayList<T>()
    for (element in this) if (predicate(element)) destination.add(element) else nonMatchElements.add(element)
    nonMatchFunc(nonMatchElements)
    return destination
}

enum class TruthTable {
    T_T, T_F, F_T, F_F
}

fun <T : Any> Iterable<T>.truthTable(predicate: (T) -> Boolean): TruthTable =
    truthTable(this.any(predicate), this.any { !predicate(it) })


fun truthTable(b1: Boolean, b2: Boolean): TruthTable = when {
    b1 && b2 -> TruthTable.T_T
    b1 && !b2 -> TruthTable.T_F
    !b1 && b2 -> TruthTable.F_T
    else -> TruthTable.F_F
}

// ========== EqualsAndHashCode ==========

fun <T : Any> T.myEquals(other: Any?, vararg properties: T.() -> Any?): Boolean {
    if (this === other)
        return true

    if (this.javaClass != other?.javaClass)
        return false

    // cast is safe, because this is T and other's class was checked for equality with T
    @Suppress("UNCHECKED_CAST")
    other as T

    return properties.all { this.it() == other.it() }
}

fun <T : Any> T.myHashCode(vararg properties: T.() -> Any?): Int {
    // Fast implementation without collection copies, based on java.util.Arrays.hashCode()
    var result = 1

    for (element in properties) {
        val value = this.element()
        result = 31 * result + (value?.hashCode() ?: 0)
    }

    return result
}