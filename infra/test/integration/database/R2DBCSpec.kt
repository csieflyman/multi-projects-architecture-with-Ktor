/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.database

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.config.ApplicationConfigLoader
import fanpoll.infra.config.MyApplicationConfig
import fanpoll.infra.database.exposed.jasync.*
import fanpoll.infra.database.exposed.sql.createdAtColumn
import fanpoll.infra.database.exposed.sql.updatedAtColumn
import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.Serializable
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.koin.test.KoinTest
import testcontainers.PostgresSQLContainerManager
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class R2DBCSpec : KoinTest, FunSpec({

    val logger = KotlinLogging.logger {}

    lateinit var appConfig: MyApplicationConfig
    lateinit var hikariConfig: fanpoll.infra.database.hikari.HikariConfig
    lateinit var dataSource: HikariDataSource
    lateinit var database: Database
    lateinit var jasyncConnection: Connection

    fun initDBTestContainers() {
        appConfig = ApplicationConfigLoader.load()
        val infraDatabaseConfig = appConfig.infra.databases.infra
        val infraDBContainer = PostgresSQLContainerManager.create("infra", hikariConfig = infraDatabaseConfig.hikari)
        infraDatabaseConfig.hikari = infraDatabaseConfig.hikari.copy(jdbcUrl = infraDBContainer.jdbcUrl)
        hikariConfig = infraDatabaseConfig.hikari
    }

    fun initExposed() {
        logger.info { "========== Exposed Init ==========" }
        dataSource = HikariDataSource(HikariConfig().apply {
            isAutoCommit = false
            driverClassName = hikariConfig.driverClassName
            jdbcUrl = hikariConfig.jdbcUrl
            username = hikariConfig.username
            password = hikariConfig.password
            maximumPoolSize = 3
        })
        database = Database.connect(dataSource)
    }

    fun initFlyway() {
        logger.info { "========== Flyway Migrate ==========" }
        val flyway = FluentConfiguration().apply {
            baselineOnMigrate(true)
        }.dataSource(dataSource)
            .locations("db/test/r2dbc_spec")
            .load()
        flyway.migrate()
    }

    fun initJasync() {
        logger.info { "========== Jasync Connect ==========" }
        jasyncConnection = PostgreSQLConnectionBuilder.createConnectionPool(hikariConfig.jdbcUrl) {
            username = hikariConfig.username
            password = hikariConfig.password
            maxActiveConnections = 3
        }
        jasyncConnection.connect().get()

        JasyncExposedAdapter.bind(database, jasyncConnection)
    }

    beforeSpec {
        initDBTestContainers()
        initExposed()
        initFlyway()
        initJasync()
    }

    afterSpec {
        logger.info { "========== Jasync Close ==========" }
        jasyncConnection.disconnect().get()

        logger.info { "========== Datasource Close ==========" }
        dataSource.close()
    }

    test("R2DBC CRUD") {
        logger.info { "========== R2DBC Test Begin ==========" }

        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()
        val createUser1 = User(user1Id, "user1", true, Gender.Male, 2000)
        val createUser2 = User(user2Id, "user2", false, Gender.Female, 2001)

        jasyncTransaction(database) {
            logger.debug { "========== Insert ==========" }
            R2DBCTestUserTable.jasyncInsert(createUser1)
            R2DBCTestUserTable.jasyncInsert(createUser2)

            val createdUser1 = R2DBCTestUserTable.selectAll()
                .where { R2DBCTestUserTable.id eq user1Id }
                .jasyncSingleDTOOrNull(UserDTO::class)
            val createdUser2 = R2DBCTestUserTable.selectAll()
                .where { R2DBCTestUserTable.id eq user2Id }
                .jasyncSingleDTOOrNull(UserDTO::class)
            assertNotNull(createdUser1)
            assertNotNull(createdUser2)

            logger.debug { "========== Update ==========" }
            val updateUser1Form = createUser1.copy(gender = Gender.Female)
            val updateUser2Form = createUser2.copy(gender = Gender.Male)
            R2DBCTestUserTable.jasyncUpdate(updateUser1Form, (R2DBCTestUserTable.id eq user1Id))
            R2DBCTestUserTable.jasyncUpdate(updateUser2Form, (R2DBCTestUserTable.id eq user2Id))
        }.await()

        logger.debug { "========== Select ==========" }
        jasyncQuery(database) {
            val user1 = R2DBCTestUserTable.selectAll()
                .where { R2DBCTestUserTable.id eq user1Id }
                .jasyncSingleDTOOrNull(UserDTO::class)
            val user2 = R2DBCTestUserTable.selectAll()
                .where { R2DBCTestUserTable.id eq user2Id }
                .jasyncSingleDTOOrNull(UserDTO::class)
            val allUsers = R2DBCTestUserTable.selectAll()
                .jasyncToList(UserDTO::class)
            assertEquals(allUsers, listOf(user1, user2))
            assertNotNull(user1)
            assertNotNull(user2)
            assertEquals(Gender.Female, user1.gender)
            assertEquals(Gender.Male, user2.gender)
            allUsers
        }.await()

        logger.debug { "========== Delete ==========" }
        jasyncTransaction(database) {
            R2DBCTestUserTable.jasyncDelete { R2DBCTestUserTable.id eq user1Id }
            R2DBCTestUserTable.jasyncDelete { R2DBCTestUserTable.id eq user2Id }
        }.await()

        jasyncQuery(database) {
            val user1 = R2DBCTestUserTable.selectAll()
                .where { R2DBCTestUserTable.id eq user1Id }
                .jasyncSingleDTOOrNull(UserDTO::class)
            val user2 = R2DBCTestUserTable.selectAll()
                .where { R2DBCTestUserTable.id eq user2Id }
                .jasyncSingleDTOOrNull(UserDTO::class)
            assertNull(user1)
            assertNull(user2)
        }.await()

        logger.info { "========== R2DBC Test End ==========" }
    }
})

object R2DBCTestUserTable : UUIDTable(name = "r2dbc_test_user") {
    val account = varchar("account", 64) //unique
    val enabled = bool("enabled")

    // gender use enumerationByName because what we get exposed statement.argValues() is string type
    val gender = enumerationByName<Gender>("gender", 10).nullable()
    val birthYear = integer("birth_year").nullable()
    val createdAt = createdAtColumn()
    val updatedAt = updatedAtColumn()

    init {
        ResultRowMappers.register(ResultRowMapper(UserDTO::class, R2DBCTestUserTable))
    }
}

enum class Gender {
    Male, Female
}

data class User(
    val id: UUID = UUID.randomUUID(),
    val account: String,
    val enabled: Boolean = true,
    val gender: Gender? = null,
    val birthYear: Int? = null
)

@Serializable
data class UserDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    var account: String? = null
    var enabled: Boolean? = null
    var gender: Gender? = null
    var birthYear: Int? = null

    @Serializable(with = InstantSerializer::class)
    var createdAt: Instant? = null

    @Serializable(with = InstantSerializer::class)
    var updatedAt: Instant? = null

    override fun getId(): UUID = id
}