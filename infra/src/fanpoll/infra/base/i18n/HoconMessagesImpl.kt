/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.i18n

import com.typesafe.config.Config
import io.ktor.server.config.tryGetString
import org.apache.commons.text.StringSubstitutor

class HoconMessagesImpl(override val lang: Lang, var config: Config) : Messages {

    override fun get(key: String, args: Map<String, Any>?): String? = config.tryGetString(key)?.let {
        (args?.let { StringSubstitutor(args) } ?: StringSubstitutor()).replace(it)
    }

    override fun isDefined(key: String): Boolean = config.hasPath(key)

    fun withFallback(another: HoconMessagesImpl) {
        config = config.withFallback(another.config)
    }
}