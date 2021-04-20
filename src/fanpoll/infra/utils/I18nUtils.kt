/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.utils

import com.neovisionaries.i18n.LanguageCode
import com.typesafe.config.ConfigFactory
import com.ufoscout.properlty.Properlty
import fanpoll.infra.ResponseCode
import fanpoll.infra.ResponseCodeType
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.channel.NotificationChannel
import io.ktor.application.ApplicationCall
import io.ktor.config.tryGetString
import io.ktor.request.ApplicationRequest
import io.ktor.request.acceptLanguage
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.apache.commons.text.StringSubstitutor

private const val LANG = "lang"
val ATTRIBUTE_KEY_LANG = AttributeKey<LanguageCode>(LANG)

object I18nUtils {

    private val logger = KotlinLogging.logger {}

    private val DEFAULT_USER_LANG = LanguageCode.zh
    private val DEFAULT_SYSTEM_LANG = LanguageCode.en

    private val notificationProperties = mapOf(
        LanguageCode.zh to Properlty.builder().add("classpath:notification/notification_zh.properties").build(),
        LanguageCode.en to Properlty.builder().add("classpath:notification/notification_en.properties").build()
    )

    fun getEmailSubject(type: NotificationType, args: Map<String, String>? = null, langCode: LanguageCode = DEFAULT_USER_LANG): String =
        getNotificationMessage(type, NotificationChannel.Email, "subject", args, langCode)

    fun getPushTitle(type: NotificationType, args: Map<String, String>? = null, langCode: LanguageCode = DEFAULT_USER_LANG): String =
        getNotificationMessage(type, NotificationChannel.Push, "title", args, langCode)

    fun getPushBody(type: NotificationType, args: Map<String, String>? = null, langCode: LanguageCode = DEFAULT_USER_LANG): String =
        getNotificationMessage(type, NotificationChannel.Push, "body", args, langCode)

    fun getSMSBody(type: NotificationType, args: Map<String, String>? = null, langCode: LanguageCode = DEFAULT_USER_LANG): String =
        getNotificationMessage(type, NotificationChannel.SMS, "body", args, langCode)

    private fun getNotificationMessage(
        type: NotificationType, channel: NotificationChannel, part: String, args: Map<String, String>? = null,
        langCode: LanguageCode
    ): String {
        val property = "${type.id}.$channel.$part"
        var value = notificationProperties[langCode]?.get(property) ?: error("notification property $property ($langCode) is undefined")
        if (args != null) {
            value = StringSubstitutor(args, "$[", "]").replace(value)
        }
        return value
    }

    // only _zh.conf now
    private val responseConfigsMap = mapOf(
        LanguageCode.zh to ConfigFactory.load("i18n/response_zh.conf"),
        LanguageCode.en to ConfigFactory.load("i18n/response_zh.conf")
    )

    fun getCodeTypeMessage(codeType: ResponseCodeType, call: ApplicationCall?): String {
        return getCodeTypeMessage(codeType, getCurrentLangCode(call))
    }

    fun getCodeTypeMessage(codeType: ResponseCodeType, langCode: LanguageCode): String {
        return getMessage("codeType.${codeType.name}", null, langCode)
    }

    private fun getCodeTypeMessageOrNull(codeType: ResponseCodeType, langCode: LanguageCode): String? {
        return getMessageOrNull("codeType.${codeType.name}", null, langCode)
    }

    fun getCodeMessage(code: ResponseCode, args: Map<String, Any>? = null, call: ApplicationCall?): String {
        return getCodeMessage(code, args, getCurrentLangCode(call))
    }

    private fun getCodeMessage(code: ResponseCode, args: Map<String, Any>? = null, langCode: LanguageCode): String {
        return getMessage("code.${code.value}", args, langCode)
    }

    fun getCodeMessageOrNull(code: ResponseCode, args: Map<String, Any>? = null, langCode: LanguageCode): String? {
        return getMessageOrNull("code.${code.value}", args, langCode)
    }

    private fun getMessage(key: String, args: Map<String, Any>?, langCode: LanguageCode): String {
        return getMessageOrNull(key, args, langCode) ?: error("message is not exist: key = $key, langCode = $langCode")
    }

    private fun getMessageOrNull(key: String, args: Map<String, Any>?, langCode: LanguageCode): String? {
        return responseConfigsMap[langCode]?.tryGetString(key)?.let {
            (args?.let { StringSubstitutor(args) } ?: StringSubstitutor()).replace(it)
        }
    }

    private fun getCurrentLangCode(call: ApplicationCall?): LanguageCode {
        return call?.attributes?.getOrNull(ATTRIBUTE_KEY_LANG) ?: DEFAULT_SYSTEM_LANG
    }

    fun getLangCodeFromCookieThenHeader(request: ApplicationRequest): LanguageCode? {
        return request.cookies[LANG]?.let { parseLangCodeString(it) }
            ?: parseLangCodeString(request.acceptLanguage())
    }

    private fun parseLangCodeString(langCodeString: String?): LanguageCode? {
        return langCodeString?.let { it ->
            try {
                LanguageCode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                logger.debug("invalid lang code value $it")
                DEFAULT_USER_LANG
            }.let { langCode ->
                if (responseConfigsMap.containsKey(langCode))
                    langCode
                else {
                    logger.debug("unsupported lang code ${langCode.name}")
                    DEFAULT_USER_LANG
                }
            }
        }
    }
}



