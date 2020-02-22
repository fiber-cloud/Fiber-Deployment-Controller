package app.fiber.image

import app.fiber.deployment.model.Deployment
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Allocates the newest snapshot image if the deployment is static.
 *
 * @property [host] Host of the Template Storage.
 *
 * @author Tammo0987
 * @since 1.0
 */
class TemplateImageAllocatorService(private val host: String) : ImageAllocatorService, KoinComponent {

    /**
     * [HttpClient] to fetch the image.
     */
    private val client by inject<HttpClient>()

    /**
     * Fetching the current image for a [deployment][Deployment].
     *
     * @param [deployment] [Deployment] to fetch the image for.
     *
     * @return Image as String.
     */
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

/**
 * Data class as response for [ImageAllocatorService.getCurrentImage].
 *
 * @property [image] Name of the image.
 *
 * @author Tammo0987
 * @since 1.0
 */
data class ImageResponse(val image: String)