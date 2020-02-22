package app.fiber.image

import app.fiber.deployment.model.Deployment
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class DockerImageAllocatorService {

    private val client = HttpClient()

    private val templateStorageHost = System.getenv("FIBER_TEMPLATE_STORAGE_SERVICE_HOST")
            ?: throw Exception("Fiber-Template-Storage host not found!")

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this.client::close))
    }

    // TODO api in template storage may not look like this
    suspend fun getNewestImage(deployment: Deployment): String {
        if (deployment.dynamic) return deployment.image

        val result = this.client.get<DockerImageResponse> {
            url {
                host = templateStorageHost
                encodedPath = "/template/api/snapshots/${deployment.uuid}"
            }
        }

        return result.image
    }

}

data class DockerImageResponse(val image: String)