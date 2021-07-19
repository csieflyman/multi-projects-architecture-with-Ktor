/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.custom

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.i18n.Lang
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.vendors.currentDialect

fun Table.principalSource(name: String): Column<PrincipalSource> = registerColumn(name, object : VarCharColumnType(30) {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): PrincipalSource = PrincipalSource.lookup(value as String)
    override fun notNullValueToDB(value: Any): Any = (value as PrincipalSource).id
})

fun Table.userType(name: String): Column<UserType> = registerColumn(name, object : VarCharColumnType(20) {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): UserType = UserType.lookup(value as String)
    override fun notNullValueToDB(value: Any): Any = (value as UserType).id
})

fun Table.lang(name: String): Column<Lang> = registerColumn(name, object : VarCharColumnType(20) {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): Lang = Lang(value as String)
    override fun notNullValueToDB(value: Any): Any = (value as Lang).code
})