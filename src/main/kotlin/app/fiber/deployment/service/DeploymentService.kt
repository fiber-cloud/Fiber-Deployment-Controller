package app.fiber.deployment.service

import app.fiber.deployment.model.Deployment

interface DeploymentService {

    suspend fun deploy(deployment: Deployment)

    suspend fun rejectDeployment(deployment: Deployment): Boolean

}