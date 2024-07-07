/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.base.extension

import io.github.oshai.kotlinlogging.KotlinLogging
import org.kpropmap.applyProps
import org.kpropmap.deserialize
import org.kpropmap.propMapOf
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

private val logger = KotlinLogging.logger {}

// ========== Map & Bean ==========
fun Any.toNotNullMap(prefix: String? = null): Map<String, Any> {
    val map = propMapOf(this).filterValuesNotNull()
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

fun <K : Any, V : Any> Map<out K?, V?>.filterValuesNotNull(): Map<K, V> {
    return filterValues { it != null } as Map<K, V>
}

fun <T : Any, R : Any> T.toObject(targetKClass: KClass<R>, block: (R.(T) -> Unit)? = null): R {
    logger.debug { "source class = " + this.javaClass.kotlin.qualifiedName }
    logger.debug { "target class = " + targetKClass.qualifiedName }
    val targetConstructor = targetKClass.primaryConstructor ?: targetKClass.constructors.first() // Assume primaryConstructor
    val target = newInstanceWithConstructor(this, targetConstructor)
    target.copyPropsFrom(this, excludes = targetConstructor.parameters.mapNotNull { it.name })
    if (block != null)
        target.block(this)
    return target
}

private fun <T : Any, R : Any> newInstanceWithConstructor(source: T, targetConstructor: KFunction<R>): R {
    val sourceProps = source.javaClass.kotlin.memberProperties
    val parameterValueMap = targetConstructor.parameters.associateWith { targetKParameter ->
        sourceProps.first { it.name == targetKParameter.name }.get(source)
    }
    logger.debug { "parameterValueMap = $parameterValueMap" }
    return targetConstructor.callBy(parameterValueMap)
}

fun <T : Any, R : Any> T.copyPropsFrom(source: R, ignoreNullValue: Boolean? = true, excludes: List<String>? = null) {
    val sourceProps = source::class.memberProperties
    val targetProps = this::class.memberProperties.filterIsInstance<KMutableProperty1<T, *>>()
        .filterNot { excludes?.contains(it.name) ?: false }
    logger.debug { "source class = " + source.javaClass.kotlin.qualifiedName }
    logger.debug { "source props =? " + sourceProps.map { it.name to it.returnType } }
    logger.debug { "target class = " + this.javaClass.kotlin.qualifiedName }
    logger.debug { "target props => " + targetProps.map { it.name to it.returnType } }

    // make sure properties have same name and compatible types
    targetProps.forEach { targetProp ->
        sourceProps.find { it.name == targetProp.name }?.let { sourceProp ->
            val value = sourceProp.getter.call(source)
            logger.debug { "copy " + sourceProp.name + " value = " + value }
            if (ignoreNullValue == false || value != null)
                targetProp.setter.call(this, value)
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