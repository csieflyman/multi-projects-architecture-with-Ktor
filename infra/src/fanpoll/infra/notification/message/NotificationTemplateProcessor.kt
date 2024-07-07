/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.message

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.i18n.Lang
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

    private val cfg: Configuration = Configuration(Configuration.VERSION_2_3_32).apply {
        templateLoader = ClassTemplateLoader(NotificationTemplateProcessor::class.java, "/i18n/templates/notification")
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

    fun processEmail(projectId: String, type: NotificationType, lang: Lang, args: Map<String, Any>): String =
        process(projectId, type.id, lang, "html", args)

    private fun process(projectId: String, typeId: String, lang: Lang, ext: String, args: Map<String, Any>): String {
        val infraTemplateFileName = "${typeId}_${lang.code}.$ext"
        val projectTemplateFileName = "${projectId}_${infraTemplateFileName}"

        //val templateFileName = "debug.html"
        //val args = mapOf("args" to args)

        val template: Template = try {
            cfg.getTemplate(projectTemplateFileName, null, null, null, true, true)
                ?: cfg.getTemplate(infraTemplateFileName)
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.DEV_ERROR, "template file $typeId parsing error or not found", e)
        }

        return try {
            val outputWriter = StringWriter()
            template.process(args, outputWriter)
            outputWriter.toString()
        } catch (e: Throwable) {
            throw InternalServerException(
                InfraResponseCode.DEV_ERROR,
                "process template error: template = ${template.name}, args = $args", e
            )
        }
    }
}