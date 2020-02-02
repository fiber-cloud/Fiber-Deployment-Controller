package app.fiber.deployment

import app.fiber.event.DeploymentDeletedEvent
import app.fiber.event.DeploymentUpdatedEvent
import app.fiber.event.EventBus
import app.fiber.image.DockerImageAllocatorService
import app.fiber.model.Deployment
import io.fabric8.kubernetes.api.model.ContainerBuilder
import io.fabric8.kubernetes.api.model.ContainerPortBuilder
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

class KubernetesDeploymentService : KoinComponent {

    private val kubernetesClient by inject<DefaultKubernetesClient>()

    private val dockerImageAllocatorService = DockerImageAllocatorService()

    init {
        val serverNameSpace = NamespaceBuilder()
                .withNewMetadata()
                .withName("fiber-deployments-server")
                .endMetadata()
                .build()

        val proxyNameSpace = NamespaceBuilder()
                .withNewMetadata()
                .withName("fiber-deployments-proxy")
                .endMetadata()
                .build()

        this.kubernetesClient.namespaces().create(serverNameSpace)
        this.kubernetesClient.namespaces().create(proxyNameSpace)
    }

    init {
        EventBus.subscribe<DeploymentUpdatedEvent> { event ->
            GlobalScope.launch { deployToKubernetes(event.deployment) }
        }

        EventBus.subscribe<DeploymentDeletedEvent> { event ->
            GlobalScope.launch { deleteKubernetesDeployment(event.deployment) }
        }
    }

    private suspend fun deployToKubernetes(deployment: Deployment) {
        val image = withContext(Dispatchers.Default) { dockerImageAllocatorService.getNewestImage(deployment) }
        val namespace = "fiber-deployments-${deployment.type}"

        val port = ContainerPortBuilder()
                .withContainerPort(25565)
                .withHostPort(25565)
                .withProtocol("TCP")
                .build()!!

        val container = ContainerBuilder()
                .withName(deployment.name)
                .withImage(image)
                .withImagePullPolicy("IfNotPresent")
                .withPorts(port)
                .withEnv(deployment.environment.map { EnvVar(it.key, it.value, null) })
                .build()

        val kubernetesDeployment = DeploymentBuilder()
                .withNewMetadata()
                .withName(deployment.name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withReplicas(deployment.minimumAmount)
                .withNewTemplate()
                .withNewMetadata()
                .withName(deployment.name)
                .withNamespace(namespace)
                .withLabels(mapOf("app" to deployment.name))
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .withRestartPolicy("Always")
                .withNodeSelector(mapOf("app" to deployment.name))
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()!!

        this.kubernetesClient.inNamespace(namespace)
                .apps()
                .deployments()
                .createOrReplace(kubernetesDeployment)
    }

    private fun deleteKubernetesDeployment(deployment: Deployment) {
        val namespace = "fiber-deployments-${deployment.type}"

        val kubernetesDeployment = this.kubernetesClient.inNamespace(namespace)
                .apps()
                .deployments()
                .withName(deployment.name)
                .get() ?: null

        if (kubernetesDeployment != null) {
            this.kubernetesClient.inNamespace(namespace)
                    .apps()
                    .deployments()
                    .delete(kubernetesDeployment)
        }
    }

}