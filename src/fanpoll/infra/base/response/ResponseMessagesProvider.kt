/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.response

import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.i18n.MessagesProvider

class ResponseMessagesProvider(messagesProvider: MessagesProvider<*>) : MessagesProvider<ResponseMessages> {

    override val messages: Map<Lang, ResponseMessages> = messagesProvider.messages
        .mapValues { ResponseMessages(it.value) }
    
}