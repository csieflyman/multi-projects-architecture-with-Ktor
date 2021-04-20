/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.controller

import io.konform.validation.Constraint
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
import org.jetbrains.exposed.sql.CharColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.VarCharColumnType
import kotlin.reflect.KProperty1

object ValidationUtils {

    const val EMAIL_MAX_LENGTH = 64

    // https://docs.microsoft.com/zh-tw/dotnet/standard/base-types/how-to-verify-that-strings-are-in-valid-email-format
    private const val EMAIL_PATTERN = """^[^@\s]+@[^@\s]+\.[^@\s]+$"""
    private val EMAIL_REGEX = EMAIL_PATTERN.toRegex()

    val EMAIL_VALIDATOR = Validation<String> {
        maxLength(EMAIL_MAX_LENGTH)
        pattern(EMAIL_REGEX)
    }

    private const val PASSWORD_MIN_LENGTH = 6
    private const val PASSWORD_MAX_LENGTH = 30
    val PASSWORD_VALIDATOR = Validation<String> {
        minLength(PASSWORD_MIN_LENGTH)
        maxLength(PASSWORD_MAX_LENGTH)
    }
}

fun <T> ValidationBuilder<T>.allNotNull(vararg properties: KProperty1<T, *>): Constraint<T> {
    return addConstraint("{0} must all not null",
        properties.joinToString(",") { it.name }) { dto ->
        properties.all { it.call(dto) != null }
    }
}

fun <T> ValidationBuilder<T>.anyNotNull(vararg properties: KProperty1<T, *>): Constraint<T> {
    return addConstraint("{0} must have at least one not null",
        properties.joinToString(",") { it.name }) { dto ->
        properties.any { it.call(dto) != null }
    }
}

fun <T> ValidationBuilder<T>.onlyOneNotNull(vararg properties: KProperty1<T, *>): Constraint<T> {
    return addConstraint("{0} must have only one not null",
        properties.joinToString(",") { it.name }) { dto ->
        properties.singleOrNull { it.call(dto) != null } != null
    }
}

fun ValidationBuilder<String>.fixLength(column: Column<String>): Constraint<String> {
    val colLength = (column.columnType as CharColumnType).colLength
    return addConstraint("must have {0} characters", colLength.toString()) { it.length == colLength }
}

fun ValidationBuilder<String>.maxLength(column: Column<String>): Constraint<String> {
    return maxLength((column.columnType as VarCharColumnType).colLength)
}

fun ValidationBuilder<String>.maxLengthIfNotNull(column: Column<String?>): Constraint<String> {
    return maxLength((column.columnType as VarCharColumnType).colLength)
}
