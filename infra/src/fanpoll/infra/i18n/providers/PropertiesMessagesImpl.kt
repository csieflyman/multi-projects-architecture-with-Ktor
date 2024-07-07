/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n.providers

import com.ufoscout.properlty.Properlty
import fanpoll.infra.i18n.Lang
import fanpoll.infra.i18n.Messages
import org.apache.commons.text.StringSubstitutor

class PropertiesMessagesImpl(override val lang: Lang, private val properties: Properlty) : Messages {

    override fun get(key: String, args: Map<String, Any>?): String? = properties[key]?.let {
        (args?.let { StringSubstitutor(args) } ?: StringSubstitutor()).replace(it)
    }

    override fun isDefined(key: String): Boolean = properties[key] != null
}