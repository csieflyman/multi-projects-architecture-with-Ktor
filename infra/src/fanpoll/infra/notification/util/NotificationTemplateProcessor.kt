/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.util

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.i18n.AvailableLangs
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.NotificationType
import freemarker.cache.ClassTemplateLoader
import freemarker.core.HTMLOutputFormat
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import java.io.StringWriter
import java.time.ZoneId
import java.util.*

class NotificationTemplateProcessor(
    private val availableLangs: AvailableLangs
) {

    private val cfg: Configuration = Configuration(Configuration.VERSION_2_3_31).apply {
        templateLoader = ClassTemplateLoader(NotificationTemplateProcessor::class.java, "/i18n/notification/templates")
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
        wrapUncheckedExceptions = true
        recognizeStandardFileExtensions = false
        defaultEncoding = "UTF-8"
        outputFormat = HTMLOutputFormat.INSTANCE
        locale = availableLangs.first().locale
        timeZone = TimeZone.getTimeZone(ZoneId.of("UTC"))
        dateFormat = "yyyy-MM-dd"
        dateTimeFormat = "yyyy-MM-dd HH:mm:ss"
    }

    fun processEmail(type: NotificationType, args: Map<String, Any>, lang: Lang): String =
        process(type.id, args, lang, "html")

    private fun process(templateName: String, args: Map<String, Any>, lang: Lang, ext: String): String {
        val templateFileName = buildTemplateFileName(templateName, lang, ext)

        //val templateFileName = "debug.html"
        //val args = mapOf("args" to args)

        val template: Template = try {
            cfg.getTemplate(templateFileName, null, null, null, true, true)
                ?: cfg.getTemplate(buildTemplateFileName(templateName, availableLangs.first(), ext))
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.DEV_ERROR, "template file $templateFileName parsing error or not found", e)
        }

        return try {
            val outputWriter = StringWriter()
            template.process(args, outputWriter)
            outputWriter.toString()
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.DEV_ERROR, "process template error: template = $templateName, args = $args", e)
        }
    }

    private fun buildTemplateFileName(templateName: String, lang: Lang, ext: String) = "${templateName}_${lang.code}.$ext"
}