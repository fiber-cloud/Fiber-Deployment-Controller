package app.fiber.deployment.service.kubernetes

import app.fiber.deployment.model.Deployment
import app.fiber.deployment.service.DeploymentService
import app.fiber.event.EventBus
import app.fiber.event.events.DeploymentDeletedEvent
import app.fiber.event.events.DeploymentUpdatedEvent
import app.fiber.image.ImageAllocatorService
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

class KubernetesDeploymentService : DeploymentService, KoinComponent {

    private val kubernetesClient by inject<DefaultKubernetesClient>()

    private val imageAllocatorService by inject<ImageAllocatorService>()

    init {
        this.createNamespace("fiber-deployments-server")
        this.createNamespace("fiber-deployments-proxy")
    }

    override suspend fun deploy(deployment: Deployment) {
        val image = withContext(Dispatchers.Default) { imageAllocatorService.getCurrentImage(deployment) }
        val namespace = "fiber-deployments-${deployment.type}"
        val kubernetesDeployment = KubernetesDeploymentBuilder(deployment, namespace, image).buildDeployment()

        this.kubernetesClient.inNamespace(namespace)
                .apps()
                .deployments()
                .createOrReplace(kubernetesDeployment)
    }

    override suspend fun rejectDeployment(deployment: Deployment): Boolean {
        val namespace = "fiber-deployments-${deployment.type}"

        return this.kubernetesClient.inNamespace(namespace)
                .apps()
                .deployments()
                .withName(deployment.name)
                .delete()
    }

    private fun createNamespace(name: String) {
        val namespace = NamespaceBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .build()

        this.kubernetesClient.namespaces().createOrReplace(namespace)
    }

}