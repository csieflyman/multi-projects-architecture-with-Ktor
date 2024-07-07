/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report.i18n

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.i18n.Lang
import fanpoll.infra.i18n.Messages
import fanpoll.infra.report.ReportType

class I18nReportMessages(private val messages: Messages) : Messages {

    fun getTitle(type: ReportType, args: Map<String, String>? = null): String = getMessage(type, "title", args)

    private fun getMessage(
        type: ReportType, part: String, args: Map<String, String>? = null
    ): String = get("${type.id}.$part", args)

    override val lang: Lang = messages.lang

    override fun get(key: String, args: Map<String, Any>?): String = messages.get(key, args)
        ?: throw InternalServerException(InfraResponseCode.DEV_ERROR, "report i18n message key $key is not found")

    override fun isDefined(key: String): Boolean = messages.isDefined(key)
}