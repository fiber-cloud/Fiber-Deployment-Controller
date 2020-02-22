package app.fiber.deployment.database

import app.fiber.deployment.model.Deployment
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheLoader
import java.time.Duration
import java.util.*

class CassandraDeploymentDatabase(private val session: CqlSession) : DeploymentDatabase {

    private val table = "deployments"

    private val cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(30))
            .build(object : CacheLoader<UUID, Deployment?>() {
                override fun load(key: UUID): Deployment? = loadDeploymentById(key)
            })!!

    init {
        this.createTable()
    }

    override fun getDeploymentById(deploymentId: UUID): Deployment? = this.cache.get(deploymentId)

    override fun getAllDeployments(): List<Deployment> {
        val selectAllDeployments = QueryBuilder.selectFrom(this.table)
                .all()
                .build()

        val result = this.session.execute(this.session.prepare(selectAllDeployments).bind())

        return result.map { row ->
            Deployment(
                    row.getUuid("deployment_id")!!,
                    row.getString("name")!!,
                    row.getString("image")!!,
                    row.getString("type")!!,
                    row.getBoolean("dynamic"),
                    row.getInt("minimum_amount"),
                    row.getInt("maximum_amount"),
                    row.getMap("environment", String::class.java, String::class.java)!!
            )
        }.all()
    }

    override suspend fun insertDeployment(deployment: Deployment) {
        this.cache.invalidate(deployment.uuid)

        val deploymentInsert = QueryBuilder.insertInto(this.table)
                .value("deployment_id", QueryBuilder.bindMarker())
                .value("name", QueryBuilder.bindMarker())
                .value("image", QueryBuilder.bindMarker())
                .value("type", QueryBuilder.bindMarker())
                .value("dynamic", QueryBuilder.bindMarker())
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
                .setBoolean(4, deployment.dynamic)
                .setInt(5, deployment.minimumAmount)
                .setInt(6, deployment.maximumAmount)
                .setMap(7, deployment.environment, String::class.java, String::class.java)

        this.session.execute(boundStatement)
    }

    override suspend fun deleteDeployment(deployment: Deployment) {
        val deploymentDelete = QueryBuilder.deleteFrom(this.table)
                .whereColumn("deployment_id")
                .isEqualTo(QueryBuilder.bindMarker())
                .build()

        val statement = this.session.prepare(deploymentDelete)
        val boundStatement = statement.bind()
                .setUuid(0, deployment.uuid)

        this.session.execute(boundStatement)
        this.cache.invalidate(deployment.uuid)
    }

    private fun createTable() {
        val tableQuery = SchemaBuilder.createTable(this.table)
                .ifNotExists()
                .withPartitionKey("deployment_id", DataTypes.UUID)
                .withColumn("name", DataTypes.TEXT)
                .withColumn("image", DataTypes.TEXT)
                .withColumn("type", DataTypes.TEXT)
                .withColumn("dynamic", DataTypes.BOOLEAN)
                .withColumn("minimum_amount", DataTypes.INT)
                .withColumn("maximum_amount", DataTypes.INT)
                .withColumn("environment", DataTypes.mapOf(DataTypes.TEXT, DataTypes.TEXT))

        this.session.execute(tableQuery.build())
    }

    private fun loadDeploymentById(deploymentId: UUID): Deployment? {
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
                    row.getBoolean("dynamic"),
                    row.getInt("minimum_amount"),
                    row.getInt("maximum_amount"),
                    row.getMap("environment", String::class.java, String::class.java)!!
            )
        }.one()
    }

}