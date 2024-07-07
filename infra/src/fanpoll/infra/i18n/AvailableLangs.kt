/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n

class AvailableLangs(val langs: List<Lang>) {
    fun first(): Lang = langs.first()
}