/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.base.form

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import kotlin.reflect.KProperty1

fun <T> ValidationBuilder<T>.allNotNull(vararg properties: KProperty1<T, *>): Constraint<T> {
    return addConstraint("{0} must all not null",
        properties.joinToString(",") { it.name }) { dto ->
        properties.all { it.call(dto) != null }
    }
}

fun <T> ValidationBuilder<T>.anyNotNull(vararg properties: KProperty1<T, *>): Constraint<T> {
    return addConstraint("{0} must at least one not null",
        properties.joinToString(",") { it.name }) { dto ->
        properties.any { it.call(dto) != null }
    }
}

fun <T> ValidationBuilder<T>.onlyOneNotNull(vararg properties: KProperty1<T, *>): Constraint<T> {
    return addConstraint("{0} must only one not null",
        properties.joinToString(",") { it.name }) { dto ->
        properties.singleOrNull { it.call(dto) != null } != null
    }
}