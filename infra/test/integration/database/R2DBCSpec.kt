/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.database

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.database.jasync.*
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.util.ResultRowDTOMapper
import integration.util.SinglePostgreSQLContainer
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.Serializable
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class R2DBCSpec : FunSpec({

    val logger = KotlinLogging.logger {}

    val postgresContainer = SinglePostgreSQLContainer.instance

    lateinit var dataSource: HikariDataSource
    lateinit var jasyncConnection: Connection

    beforeSpec {
        logger.info { "========== Exposed Init ==========" }
        dataSource = HikariDataSource(HikariConfig().apply {
            isAutoCommit = false
            driverClassName = postgresContainer.driverClassName
            jdbcUrl = postgresContainer.jdbcUrl
            username = postgresContainer.username
            password = postgresContainer.password
            maximumPoolSize = 3
        })
        val defaultDatabase = Database.connect(dataSource)

        logger.info { "========== Flyway Migrate ==========" }
        val flyway = FluentConfiguration().apply {
            baselineOnMigrate(true)
        }.dataSource(dataSource)
            .locations("db/test/r2dbc_spec")
            .load()
        flyway.migrate()

        logger.info { "========== Jasync Connect ==========" }
        jasyncConnection = PostgreSQLConnectionBuilder.createConnectionPool(postgresContainer.jdbcUrl) {
            username = postgresContainer.username
            password = postgresContainer.password
            maxActiveConnections = 3
        }
        jasyncConnection.connect().get()

        JasyncExposedAdapter.bind(defaultDatabase, jasyncConnection)
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
        val createUser1Form = UserForm(user1Id, "user1", true, Gender.Male, 2000)
        val createUser2Form = UserForm(user2Id, "user2", false, Gender.Female, 2001)

        dbTransactionAsync {
            logger.debug { "========== Insert ==========" }
            R2DBCTestUserTable.insertAsync(createUser1Form)
            R2DBCTestUserTable.insertAsync(createUser2Form)

            val createdUser1 = R2DBCTestUserTable.select { R2DBCTestUserTable.id eq user1Id }.toSingleDTOOrNull<UserDTO>()
            val createdUser2 = R2DBCTestUserTable.select { R2DBCTestUserTable.id eq user2Id }.toSingleDTOOrNull<UserDTO>()
            assertNotNull(createdUser1)
            assertNotNull(createdUser2)

            logger.debug { "========== Update ==========" }
            val updateUser1Form = createUser1Form.copy(gender = Gender.Female)
            val updateUser2Form = createUser2Form.copy(gender = Gender.Male)
            R2DBCTestUserTable.updateAsync(updateUser1Form)
            R2DBCTestUserTable.updateAsync(updateUser2Form)

            val updatedUser1 = R2DBCTestUserTable.select { R2DBCTestUserTable.id eq user1Id }.toSingleDTOOrNull<UserDTO>()
            val updatedUser2 = R2DBCTestUserTable.select { R2DBCTestUserTable.id eq user2Id }.toSingleDTOOrNull<UserDTO>()
            assertNotNull(updatedUser1)
            assertNotNull(updatedUser2)
            assertEquals(Gender.Female, updatedUser1.gender)
            assertEquals(Gender.Male, updatedUser2.gender)
        }

        logger.debug { "========== Select ==========" }
        dbQueryAsync {
            val user1 = R2DBCTestUserTable.select { R2DBCTestUserTable.id eq user1Id }.toSingleDTOOrNull<UserDTO>()
            val user2 = R2DBCTestUserTable.select { R2DBCTestUserTable.id eq user2Id }.toSingleDTOOrNull<UserDTO>()
            val allUsers = R2DBCTestUserTable.selectAll().toDTO<UserDTO>()
            assertEquals(allUsers, listOf(user1, user2))
            allUsers
        }.await()

        logger.debug { "========== Delete ==========" }
        dbTransactionAsync {
            R2DBCTestUserTable.deleteAsync { R2DBCTestUserTable.id eq user1Id }
            R2DBCTestUserTable.deleteAsync { R2DBCTestUserTable.id eq user2Id }

            val createdUser1 = R2DBCTestUserTable.select { R2DBCTestUserTable.id eq user1Id }.toSingleDTOOrNull<UserDTO>()
            val createdUser2 = R2DBCTestUserTable.select { R2DBCTestUserTable.id eq user2Id }.toSingleDTOOrNull<UserDTO>()
            assertNull(createdUser1)
            assertNull(createdUser2)
        }

        logger.info { "========== R2DBC Test End ==========" }
    }
})

object R2DBCTestUserTable : UUIDTable(name = "r2dbc_test_user") {
    val account = varchar("account", 64) //unique
    val enabled = bool("enabled")
    val gender = enumeration("gender", Gender::class).nullable()
    val birthYear = integer("birth_year").nullable()
    val createdAt = timestamp("created_at")
        .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp())
    val updatedAt = timestamp("updated_at")
        .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp())

    override val naturalKeys: List<Column<out Any>> = listOf(account)
    override val surrogateKey: Column<EntityID<UUID>> = id
}

enum class Gender {
    Male, Female
}

data class UserForm(
    val id: UUID = UUID.randomUUID(),
    val account: String,
    val enabled: Boolean = true,
    val gender: Gender? = null,
    val birthYear: Int? = null
) : EntityForm<UserForm, String, UUID>() {

    override fun getEntityId(): UUID = id

    override fun getDtoId(): String = account
}

@Serializable
data class UserDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    var account: String? = null
    var enabled: Boolean? = null
    var gender: Gender? = null
    var birthYear: Int? = null

    @Serializable(with = InstantSerializer::class)
    var createdAt: Instant? = null

    @Serializable(with = InstantSerializer::class)
    var updateAt: Instant? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<UserDTO> = ResultRowDTOMapper(UserDTO::class, R2DBCTestUserTable)
    }
}