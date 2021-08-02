/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.i18n

import com.ufoscout.properlty.Properlty
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode

class PropertiesMessagesProvider(availableLangs: AvailableLangs, basePackagePath: String, filePrefix: String) :
    MessagesProvider<PropertiesMessagesImpl> {

    override val messages: Map<Lang, PropertiesMessagesImpl> = availableLangs.langs.associateWith {
        PropertiesMessagesImpl(
            it, Properlty.builder().add("classpath:$basePackagePath/$filePrefix$it.properties")
                .ignoreUnresolvablePlaceholders(true)
                .build()
        )
    }

    init {
        val defaultLang = availableLangs.first()
        if (!messages.containsKey(defaultLang))
            throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "default lang file $basePackagePath/$filePrefix${defaultLang.code}.properties is missing"
            )
    }
}