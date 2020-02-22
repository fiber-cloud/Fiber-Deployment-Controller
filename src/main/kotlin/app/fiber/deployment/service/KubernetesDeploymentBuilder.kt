package app.fiber.deployment.service

import app.fiber.deployment.model.Deployment
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder

class KubernetesDeploymentBuilder(
        private val deployment: Deployment,
        private val namespace: String,
        private val image: String
) {

    fun buildDeployment(): io.fabric8.kubernetes.api.model.apps.Deployment {
        return DeploymentBuilder()
                .withMetadata(this.buildMetadata())
                .withSpec(this.buildDeploymentSpec())
                .build()
    }

    private fun buildMetadata(): ObjectMeta {
        return ObjectMetaBuilder()
                .withName(this.deployment.name)
                .withNamespace(this.namespace)
                .withLabels(mapOf("app" to this.deployment.name))
                .build()
    }

    private fun buildDeploymentSpec(): DeploymentSpec {
        return DeploymentSpecBuilder()
                .withReplicas(this.deployment.minimumAmount)
                .withTemplate(this.buildTemplate())
                .build()
    }

    private fun buildTemplate(): PodTemplateSpec {
        return PodTemplateSpecBuilder()
                .withMetadata(this.buildMetadata())
                .withSpec(this.buildPodSpec())
                .build()
    }

    private fun buildPodSpec(): PodSpec {
        return PodSpecBuilder()
                .withContainers(this.buildContainer())
                .withRestartPolicy("Always")
                .withNodeSelector(mapOf("app" to this.deployment.name))
                .build()
    }

    private fun buildContainer(): Container {
        return ContainerBuilder()
                .withName(this.deployment.name)
                .withImage(this.image)
                .withImagePullPolicy("IfNotPresent")
                .withPorts(this.buildPort())
                .withEnv(this.deployment.environment.map { EnvVar(it.key, it.value, null) })
                .build()
    }

    private fun buildPort(): ContainerPort {
        return ContainerPortBuilder()
                .withContainerPort(25565)
                .withHostPort(25565)
                .withProtocol("TCP")
                .build()
    }

}