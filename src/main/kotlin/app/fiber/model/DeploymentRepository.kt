package app.fiber.model

import app.fiber.event.DeploymentDeletedEvent
import app.fiber.event.DeploymentUpdatedEvent
import app.fiber.event.EventBus
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheLoader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.*

class DeploymentRepository(private val session: CqlSession) {

    private val table = "deployments"

    private val cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(30))
            .build(object : CacheLoader<UUID, Deployment?>() {
                override fun load(key: UUID): Deployment? = getDeploymentById(key)
            })!!

    init {
        this.createTable()

        GlobalScope.launch { deployAll() }
    }

    fun insertDeployment(deployment: Deployment) {
        this.cache.invalidate(deployment.uuid)

        val deploymentInsert = QueryBuilder.insertInto(this.table)
                .value("deployment_id", QueryBuilder.bindMarker())
                .value("name", QueryBuilder.bindMarker())
                .value("image", QueryBuilder.bindMarker())
                .value("type", QueryBuilder.bindMarker())
                .value("minimum_amount", QueryBuilder.bindMarker())
                .value("maximum_amount", QueryBuilder.bindMarker())
                .value("environment", QueryBuilder.bindMarker())
                .build()

        val statement = this.session.prepare(deploymentInsert)

        val boundStatement = statement.bind()
                .setUuid(0, deployment.uuid)
                .setString(1, deployment.name)
                .setString(2, deployment.image)
                .setString(3, deployment.type)
                .setInt(4, deployment.minimumAmount)
                .setInt(5, deployment.maximumAmount)
                .setMap(6, deployment.environment, String::class.java, String::class.java)

        this.session.execute(boundStatement)

        EventBus.fire(DeploymentUpdatedEvent(deployment))
    }

    fun getDeploymentById(deploymentId: UUID): Deployment? {
        val deploymentSelect = QueryBuilder.selectFrom(this.table)
                .all()
                .whereColumn("deployment_id")
                .isEqualTo(QueryBuilder.bindMarker())
                .build()

        val statement = this.session.prepare(deploymentSelect)
        val boundStatement = statement.bind()
                .setUuid(0, deploymentId)

        val result = this.session.execute(boundStatement)

        return result.map { row ->
            Deployment(
                    row.getUuid("deployment_id")!!,
                    row.getString("name")!!,
                    row.getString("image")!!,
                    row.getString("type")!!,
                    row.getInt("minimum_amount"),
                    row.getInt("maximum_amount"),
                    row.getMap("environment", String::class.java, String::class.java)!!
            )
        }.one()
    }

    fun deleteDeployment(deployment: Deployment) {
        val deploymentDelete = QueryBuilder.deleteFrom(this.table)
                .whereColumn("deployment_id")
                .isEqualTo(QueryBuilder.bindMarker())
                .build()

        val statement = this.session.prepare(deploymentDelete)
        val boundStatement = statement.bind()
                .setUuid(0, deployment.uuid)

        this.session.execute(boundStatement)
        this.cache.invalidate(deployment.uuid)

        EventBus.fire(DeploymentDeletedEvent(deployment))
    }

    private fun createTable() {
        val tableQuery = SchemaBuilder.createTable(this.table)
                .ifNotExists()
                .withPartitionKey("deployment_id", DataTypes.UUID)
                .withColumn("name", DataTypes.TEXT)
                .withColumn("image", DataTypes.TEXT)
                .withColumn("type", DataTypes.TEXT)
                .withColumn("minimum_amount", DataTypes.INT)
                .withColumn("maximum_amount", DataTypes.INT)
                .withColumn("environment", DataTypes.mapOf(DataTypes.TEXT, DataTypes.TEXT))

        this.session.execute(tableQuery.build())
    }

    private fun deployAll() {
        val getAllKeysQuery = QueryBuilder.selectFrom(this.table)
                .column("deployment_id")
                .build()

        val statement = this.session.prepare(getAllKeysQuery)
        val result = this.session.execute(statement.bind())

        result.forEach { row ->
            GlobalScope.launch {
                row.getUuid("deployment_id")?.let { id ->
                    getDeploymentById(id)?.let { deployment ->
                        EventBus.fire(DeploymentUpdatedEvent(deployment))
                    }
                }
            }
        }
    }

}