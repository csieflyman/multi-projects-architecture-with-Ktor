/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */
package fanpoll.infra.utils

fun <K : Any, V : Any> Map<out K?, V?>.filterNotNull(): Map<K, V> {
    return filterValues { it != null } as Map<K, V>
}

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

enum class TruthTable {
    T_T, T_F, F_T, F_F
}

fun <T : Any> Collection<T>.truthTable(predicate: (T) -> Boolean): TruthTable =
    truthTable(this.any(predicate), this.any { !predicate(it) })


fun truthTable(b1: Boolean, b2: Boolean): TruthTable = when {
    b1 && b2 -> TruthTable.T_T
    b1 && !b2 -> TruthTable.T_F
    !b1 && b2 -> TruthTable.F_T
    else -> TruthTable.F_F
}