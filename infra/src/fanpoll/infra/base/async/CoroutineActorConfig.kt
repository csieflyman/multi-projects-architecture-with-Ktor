/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.async

data class CoroutineActorConfig(val coroutines: Int = 1, val dispatcher: ThreadPoolConfig? = null)