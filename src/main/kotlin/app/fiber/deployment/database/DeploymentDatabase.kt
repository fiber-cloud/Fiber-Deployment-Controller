package app.fiber.deployment.database

import app.fiber.deployment.model.Deployment
import java.util.*

interface DeploymentDatabase {

    fun getDeploymentById(deploymentId: UUID): Deployment?

    fun getAllDeployments(): List<Deployment>

    suspend fun insertDeployment(deployment: Deployment)

    suspend fun deleteDeployment(deployment: Deployment)

}