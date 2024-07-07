/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n.providers

import com.ufoscout.properlty.Properlty
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.i18n.Lang
import fanpoll.infra.i18n.MessagesProvider

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