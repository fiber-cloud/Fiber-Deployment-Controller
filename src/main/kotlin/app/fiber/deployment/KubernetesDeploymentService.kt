package app.fiber.deployment

import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.koin.core.KoinComponent
import org.koin.core.inject

class KubernetesDeploymentService : KoinComponent {

    private val kubernetesClient by inject<DefaultKubernetesClient>()

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

}