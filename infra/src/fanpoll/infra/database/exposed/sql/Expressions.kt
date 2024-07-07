/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionAlias
import org.jetbrains.exposed.sql.QueryBuilder

class RawExpression(private val raw: String) : Expression<String>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder { +raw }
}

class CountExpression : Expression<Long>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder { +"COUNT(*)" }
}

class NullableValueExpressionAlias<T : Any?>(private val exprAlias: ExpressionAlias<T>) : Expression<T?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = exprAlias.toQueryBuilder(queryBuilder)
}