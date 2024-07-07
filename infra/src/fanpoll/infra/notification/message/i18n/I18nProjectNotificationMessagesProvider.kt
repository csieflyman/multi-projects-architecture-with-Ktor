/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.message.i18n

import fanpoll.infra.i18n.Lang

class I18nProjectNotificationMessagesProvider {

    private val providers: MutableMap<String, I18nNotificationMessagesProvider> = mutableMapOf()

    fun addProvider(projectId: String, provider: I18nNotificationMessagesProvider) {
        providers[projectId] = provider
    }

    fun getMessages(projectId: String, lang: Lang): I18nNotificationMessages =
        providers[projectId]!!.preferred(lang)
}