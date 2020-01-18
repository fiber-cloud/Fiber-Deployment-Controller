package app.fiber.model

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheLoader
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
    }

    fun insertDeployment(deployment: Deployment) {
        this.cache.invalidate(deployment.uuid)

        val deploymentInsert = QueryBuilder.insertInto(this.table)
                .value("deployment_id", QueryBuilder.bindMarker())
                .value("name", QueryBuilder.bindMarker())
                .value("template", QueryBuilder.bindMarker())
                .value("minimum_amount", QueryBuilder.bindMarker())
                .value("maximum_amount", QueryBuilder.bindMarker())
                .value("jvm_configuration", QueryBuilder.bindMarker())
                .value("start_parameters", QueryBuilder.bindMarker())
                .value("system_properties", QueryBuilder.bindMarker())
                .value("environment", QueryBuilder.bindMarker())
                .build()

        val statement = this.session.prepare(deploymentInsert)

        val boundStatement = statement.bind()
                .setUuid(0, deployment.uuid)
                .setString(1, deployment.name)
                .setString(2, deployment.template)
                .setInt(3, deployment.minimumAmount)
                .setInt(4, deployment.maximumAmount)
                .setList(5, deployment.jvmConfiguration, String::class.java)
                .setList(6, deployment.startParameters, String::class.java)
                .setMap(7, deployment.systemProperties, String::class.java, String::class.java)
                .setMap(8, deployment.environment, String::class.java, String::class.java)

        this.session.execute(boundStatement)
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
                    row.getString("template")!!,
                    row.getInt("minimum_amount"),
                    row.getInt("maximum_amount"),
                    row.getList("jvm_configuration", String::class.java)!!,
                    row.getList("start_parameters", String::class.java)!!,
                    row.getMap("system_properties", String::class.java, String::class.java)!!,
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
    }

    private fun createTable() {
        val tableQuery = SchemaBuilder.createTable(this.table)
                .ifNotExists()
                .withPartitionKey("deployment_id", DataTypes.UUID)
                .withColumn("name", DataTypes.TEXT)
                .withColumn("template", DataTypes.TEXT)
                .withColumn("minimum_amount", DataTypes.INT)
                .withColumn("maximum_amount", DataTypes.INT)
                .withColumn("jvm_configuration", DataTypes.listOf(DataTypes.TEXT))
                .withColumn("start_parameters", DataTypes.listOf(DataTypes.TEXT))
                .withColumn("system_properties", DataTypes.mapOf(DataTypes.TEXT, DataTypes.TEXT))
                .withColumn("environment", DataTypes.mapOf(DataTypes.TEXT, DataTypes.TEXT))

        this.session.execute(tableQuery.build())
    }

}