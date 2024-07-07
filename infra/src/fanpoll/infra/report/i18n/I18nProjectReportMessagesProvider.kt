/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report.i18n

import fanpoll.infra.i18n.Lang

class I18nProjectReportMessagesProvider {

    private val providers: MutableMap<String, I18nReportMessagesProvider> = mutableMapOf()

    fun addProvider(projectId: String, provider: I18nReportMessagesProvider) {
        providers[projectId] = provider
    }

    fun getMessages(projectId: String, lang: Lang): I18nReportMessages =
        providers[projectId]!!.preferred(lang)
}