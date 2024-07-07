/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report.i18n

import fanpoll.infra.i18n.Lang
import fanpoll.infra.i18n.MessagesProvider

class I18nReportMessagesProvider(messagesProvider: MessagesProvider<*>) : MessagesProvider<I18nReportMessages> {

    override val messages: Map<Lang, I18nReportMessages> = messagesProvider.messages
        .mapValues { I18nReportMessages(it.value) }
}