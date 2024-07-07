/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n

interface Messages {

    val lang: Lang

    fun get(key: String, args: Map<String, Any>? = null): String?

    fun isDefined(key: String): Boolean

}