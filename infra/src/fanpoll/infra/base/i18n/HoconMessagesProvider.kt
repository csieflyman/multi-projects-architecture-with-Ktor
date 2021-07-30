/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.i18n

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.ResponseCode

class HoconMessagesProvider(availableLangs: AvailableLangs, basePackagePath: String, filePrefix: String, allowUnresolved: Boolean = false) :
    MessagesProvider<HoconMessagesImpl> {

    override val messages: Map<Lang, HoconMessagesImpl> = availableLangs.langs.associateWith {
        HoconMessagesImpl(
            it, ConfigFactory.load(
                "$basePackagePath/$filePrefix$it.conf",
                ConfigParseOptions.defaults().apply { allowMissing = false },
                ConfigResolveOptions.defaults().setAllowUnresolved(allowUnresolved)
            )
        )
    }

    init {
        val defaultLang = availableLangs.first()
        if (!messages.containsKey(defaultLang))
            throw InternalServerException(
                ResponseCode.SERVER_CONFIG_ERROR,
                "default lang file $basePackagePath/$filePrefix${defaultLang.code}.conf is missing"
            )
    }
}