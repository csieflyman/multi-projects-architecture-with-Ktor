/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import org.jetbrains.exposed.sql.Count
import org.jetbrains.exposed.sql.Function

fun Function<*>.countDistinct(): Count = Count(this, true)