/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.Statement

fun Query.argValues(): List<Any?> = getStatementArgs(this)

fun Statement<*>.argValues(): List<Any?> = getStatementArgs(this)

private fun getStatementArgs(statement: Statement<*>): List<Any?> = when (statement) {
    is InsertStatement<*> -> statement.arguments().takeIf { it.isNotEmpty() }?.let { it[0] }
    else -> statement.arguments().takeIf { it.any() }?.first()
}?.map { pair -> pair.second }?.toList() ?: emptyList()