/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.i18n

import com.ufoscout.properlty.Properlty
import org.apache.commons.text.StringSubstitutor

class PropertiesMessagesImpl(override val lang: Lang, private val properties: Properlty) : Messages {

    override fun get(key: String, args: Map<String, Any>?): String? = properties[key]?.let {
        (args?.let { StringSubstitutor(args) } ?: StringSubstitutor()).replace(it)
    }

    override fun isDefined(key: String): Boolean = properties[key] != null
}