/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import com.google.common.base.CaseFormat
import org.jetbrains.exposed.sql.Column

val Column<*>.propName: String
    get() = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this.name)