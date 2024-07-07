/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra

import com.typesafe.config.Config
import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode

class Project(val id: String, val config: Config) {
    override fun equals(other: Any?) = myEquals(other, { id })
    override fun hashCode() = myHashCode({ id })
    override fun toString(): String = id
}