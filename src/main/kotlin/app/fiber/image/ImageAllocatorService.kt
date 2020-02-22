package app.fiber.image

import app.fiber.deployment.model.Deployment

interface ImageAllocatorService {

    suspend fun getCurrentImage(deployment: Deployment): String

}