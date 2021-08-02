/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.response

import fanpoll.infra.base.exception.BaseException
import fanpoll.infra.base.exception.EntityException
import fanpoll.infra.base.extension.toNotNullMap
import fanpoll.infra.base.i18n.HoconMessagesImpl
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.i18n.Messages

class ResponseMessages(val messages: HoconMessagesImpl) : Messages {

    fun getMessage(ex: BaseException): String =
        if (ex.code.isError()) getCodeTypeMessage(ex.code.type)
        else getCodeMessage(ex)

    fun getDetailMessage(ex: BaseException): String {
        val message = if (ex.code.isError()) getCodeMessage(ex) else ""
        return ex.message?.let { if (message.isNotEmpty()) "$message => $it" else it } ?: message
    }

    private fun getCodeTypeMessage(codeType: ResponseCodeType): String {
        return get("codeType.${codeType.name}", null)!!
    }

    private fun getCodeMessage(ex: BaseException): String {
        return if (ex is EntityException) {
            val args: MutableMap<String, Any> = mutableMapOf()

            if (ex.entity != null) {
                args.putAll(ex.entity.toNotNullMap("entity"))
            }

            if (ex.dataMap != null) {
                args.putAll(ex.dataMap)
            }
            getCodeMessage(ex.code, args)
        } else {
            getCodeMessage(ex.code, ex.dataMap)
        }
    }

    private fun getCodeMessage(code: ResponseCode, args: Map<String, Any>? = null): String {
        val message = get("code.${code.value}", args)
        if (code.type == ResponseCodeType.CLIENT_INFO)
            requireNotNull(message)
        return message ?: ""
    }

    override val lang: Lang = messages.lang

    override fun get(key: String, args: Map<String, Any>?): String? = messages.get(key, args)

    override fun isDefined(key: String): Boolean = messages.isDefined(key)
}