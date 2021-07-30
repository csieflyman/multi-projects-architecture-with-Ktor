/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.async

data class CoroutineActorConfig(val coroutines: Int = 1, val dispatcher: ThreadPoolConfig? = null) {

    fun validate() {
        dispatcher?.validate()
    }

    class Builder {

        var coroutines: Int = 1
        private var dispatcher: ThreadPoolConfig? = null

        fun dispatcher(block: ThreadPoolConfig.Builder.() -> Unit) {
            dispatcher = ThreadPoolConfig.Builder().apply(block).build()
        }

        fun build(): CoroutineActorConfig {
            return CoroutineActorConfig(coroutines, dispatcher).apply { validate() }
        }
    }
}