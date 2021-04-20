/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.utils

import com.neovisionaries.i18n.LanguageCode
import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.notification.NotificationType
import freemarker.cache.ClassTemplateLoader
import freemarker.core.HTMLOutputFormat
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import java.io.StringWriter
import java.time.ZoneId
import java.util.*

object NotificationTemplateProcessor {

    private val cfg: Configuration = Configuration(Configuration.VERSION_2_3_31).apply {
        templateLoader = ClassTemplateLoader(NotificationTemplateProcessor::class.java, "/notification/templates")
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
        wrapUncheckedExceptions = true
        recognizeStandardFileExtensions = false
        defaultEncoding = "UTF-8"
        outputFormat = HTMLOutputFormat.INSTANCE
        timeZone = TimeZone.getTimeZone(ZoneId.of("UTC"))
        dateFormat = "yyyy-MM-dd"
        dateTimeFormat = "yyyy-MM-dd HH:mm:ss"
    }

    fun processEmail(type: NotificationType, dataModel: Map<String, Any>, langCode: LanguageCode = LanguageCode.zh): String =
        process(type.id, "html", dataModel, langCode)

    private fun process(templateName: String, ext: String, dataModel: Map<String, Any>, langCode: LanguageCode = LanguageCode.zh): String {
        val templateFileName = "${templateName}_${langCode.name}.$ext"

        val template: Template = try {
            cfg.getTemplate(templateFileName)
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.DEV_ERROR, "template file $templateFileName parsing error or not found", e)
        }

        val outputWriter = StringWriter()
        try {
            template.process(dataModel, outputWriter)
        } catch (e: Throwable) {
            throw InternalServerErrorException(
                ResponseCode.DEV_ERROR, "process template error: template = $templateName, dataModel = $dataModel", e
            )
        }
        return outputWriter.toString()
    }
}