/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.i18n

import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.notification.NotificationType

class I18nNotificationProjectMessages {

    private val providers: MutableMap<String, I18nNotificationMessagesProvider> = mutableMapOf()

    fun addProvider(projectId: String, provider: I18nNotificationMessagesProvider) {
        providers[projectId] = provider
    }

    fun getMessages(notificationType: NotificationType, lang: Lang): I18nNotificationMessages =
        providers[notificationType.projectId]!!.preferred(lang)
}