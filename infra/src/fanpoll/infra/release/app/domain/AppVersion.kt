/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.domain

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import io.konform.validation.Validation
import io.konform.validation.jsonschema.pattern
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class AppVersion(val appId: String, val os: AppOS, val name: String) : Comparable<AppVersion> {

    @Transient
    val number: Int = nameToNumber(name)

    override fun compareTo(other: AppVersion): Int = number - other.number

    override fun toString(): String {
        return "$appId/$os/$name"
    }

    companion object {

        const val NAME_PATTERN = """^(\d)\.(\d)\.(\d{1,2})$"""

        private val NAME_REGEX = NAME_PATTERN.toRegex()

        val NAME_VALIDATOR = Validation {
            pattern(NAME_REGEX)
        }

        fun nameToNumber(name: String): Int {
            return NAME_REGEX.find(name)?.let {
                val (major, minor, patch) = it.destructured
                major.toInt() * 1000 + minor.toInt() * 100 + patch.toInt()
            } ?: throw RequestException(InfraResponseCode.BAD_REQUEST_APP_VERSION, "invalid appVersion name format: $name")
        }
    }
}