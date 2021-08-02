/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.i18n

import fanpoll.infra.base.config.ValidateableConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import org.apache.commons.lang3.LocaleUtils
import java.util.*

data class I18nConfig(val langs: List<String>? = null) : ValidateableConfig {

    override fun validate() {
        if (langs != null) {
            require(langs.isNotEmpty()) { "i18n langs should not be empty" }
            langs.forEach { tag ->
                LocaleUtils.isAvailableLocale(
                    try {
                        Locale.Builder().setLanguageTag(tag).build()
                    } catch (e: IllformedLocaleException) {
                        throw InternalServerException(InfraResponseCode.SERVER_CONFIG_ERROR, "invalid i18n lang: $tag")
                    }
                )
            }
        }
    }

    fun availableLangs(): AvailableLangs? = langs?.let { AvailableLangs(it.map { tag -> Lang(tag) }) }
}