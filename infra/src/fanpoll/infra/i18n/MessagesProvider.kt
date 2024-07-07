/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n

interface MessagesProvider<T : Messages> {

    val messages: Map<Lang, T>

    val langs: List<Lang>
        get() = messages.keys.toList()

    operator fun get(lang: Lang) = messages[lang]

    private fun preferredWithFallback(candidates: List<Lang>): T {
        val availables = messages.keys
        val lang = candidates.firstOrNull { candidate -> availables.firstOrNull { it.satisfies(candidate) } != null }
            ?: availables.first()
        return messages[lang]!!
    }

    fun preferred(candidates: List<Lang>? = null): T = preferredWithFallback(candidates ?: langs)

    fun preferred(lang: Lang? = null): T = preferred(lang?.let { listOf(it) })
}