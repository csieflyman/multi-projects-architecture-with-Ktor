/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.form

import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern

object ValidationUtils {

    // https://docs.microsoft.com/zh-tw/dotnet/standard/base-types/how-to-verify-that-strings-are-in-valid-email-format
    const val EMAIL_MAX_LENGTH = 64
    private const val EMAIL_PATTERN = """^[^@\s]+@[^@\s]+\.[^@\s]+$"""
    private val EMAIL_REGEX = EMAIL_PATTERN.toRegex()
    val EMAIL_VALIDATOR = Validation {
        maxLength(EMAIL_MAX_LENGTH)
        pattern(EMAIL_REGEX)
    }

    const val MOBILE_NUMBER_LENGTH = 15
    private const val MOBILE_NUMBER_PATTERN = """^(\+\d{1,3})?\d{10}$"""
    private val MOBILE_NUMBER_REGEX = MOBILE_NUMBER_PATTERN.toRegex()
    val MOBILE_NUMBER_VALIDATOR = Validation {
        pattern(MOBILE_NUMBER_REGEX)
    }

    private const val PASSWORD_MIN_LENGTH = 6
    private const val PASSWORD_MAX_LENGTH = 30
    val PASSWORD_VALIDATOR = Validation {
        minLength(PASSWORD_MIN_LENGTH)
        maxLength(PASSWORD_MAX_LENGTH)
    }
}
