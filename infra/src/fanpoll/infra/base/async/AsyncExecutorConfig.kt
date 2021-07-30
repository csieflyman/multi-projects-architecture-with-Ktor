/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.async

data class AsyncExecutorConfig(val coroutineActor: CoroutineActorConfig) {

    class Builder {

        private lateinit var coroutineActor: CoroutineActorConfig

        fun coroutineActor(block: CoroutineActorConfig.Builder.() -> Unit) {
            coroutineActor = CoroutineActorConfig.Builder().apply(block).build()
        }

        fun build(): AsyncExecutorConfig {
            return AsyncExecutorConfig(coroutineActor)
        }
    }
}
