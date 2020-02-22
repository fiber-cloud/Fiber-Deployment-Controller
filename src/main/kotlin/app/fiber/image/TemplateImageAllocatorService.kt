package app.fiber.image

import app.fiber.deployment.model.Deployment
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class TemplateImageAllocatorService(private val host: String) : ImageAllocatorService {

    private val client = HttpClient()

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this.client::close))
    }

    override suspend fun getCurrentImage(deployment: Deployment): String {
        if (deployment.dynamic) return deployment.image

        val result = this.client.get<ImageResponse> {
            url {
                host = this@TemplateImageAllocatorService.host
                encodedPath = "/template/api/snapshots/${deployment.uuid}"
            }
        }

        return result.image
    }

}

data class ImageResponse(val image: String)