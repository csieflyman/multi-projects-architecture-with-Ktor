/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.base.datetime

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Instant.toEpocMicro(): Long = ChronoUnit.MICROS.between(Instant.EPOCH, this)
fun Instant.toEpocNano(): Long = ChronoUnit.NANOS.between(Instant.EPOCH, this)
fun Duration.toMicros(): Long = this.toNanos() / 1000