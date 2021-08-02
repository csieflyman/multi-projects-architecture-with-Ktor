/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.response

import fanpoll.infra.base.i18n.HoconMessagesProvider
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.i18n.MessagesProvider
import mu.KotlinLogging

class ResponseMessagesProvider(messagesProvider: HoconMessagesProvider) : MessagesProvider<ResponseMessages> {

    private val logger = KotlinLogging.logger {}

    override val messages: Map<Lang, ResponseMessages> = messagesProvider.messages
        .mapValues { ResponseMessages(it.value) }

    fun merge(another: HoconMessagesProvider) {
        //logger.debug { "========== ResponseMessagesProvider withFallback ==========" }
        messages.forEach { (lang, responseMessages) ->
            another.messages[lang]?.let {
                responseMessages.messages.withFallback(it)
            }
            //logger.debug { "$lang => ${responseMessages.messages.config.entrySet().filter { it.key.startsWith("code.") }}" }
        }
    }

}