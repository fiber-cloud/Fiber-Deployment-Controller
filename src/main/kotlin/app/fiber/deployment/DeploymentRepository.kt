package app.fiber.deployment

import app.fiber.deployment.database.DeploymentDatabase
import app.fiber.deployment.model.Deployment
import app.fiber.deployment.service.DeploymentService
import app.fiber.event.EventBus
import app.fiber.event.events.DeploymentDeletedEvent
import app.fiber.event.events.DeploymentUpdatedEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*

class DeploymentRepository : KoinComponent {

    private val deploymentDatabase by inject<DeploymentDatabase>()

    private val deploymentService by inject<DeploymentService>()

    init {
        EventBus.subscribe<DeploymentUpdatedEvent> { event ->
            this.deploymentService.deploy(event.deployment)
        }

        EventBus.subscribe<DeploymentDeletedEvent> { event ->
            this.deploymentService.rejectDeployment(event.deployment)
        }

        this.deploymentDatabase.getAllDeployments().forEach {
            EventBus.fire(DeploymentUpdatedEvent(it))
        }
    }

    fun insertDeployment(deployment: Deployment) {
        GlobalScope.launch {
            this@DeploymentRepository.deploymentDatabase.insertDeployment(deployment)
        }

        EventBus.fire(DeploymentUpdatedEvent(deployment))
    }

    fun getDeploymentById(deploymentId: UUID): Deployment? = this.deploymentDatabase.getDeploymentById(deploymentId)

    fun getAllDeployments(): List<Deployment> = this.deploymentDatabase.getAllDeployments()

    fun deleteDeployment(deployment: Deployment) {
        GlobalScope.launch {
            this@DeploymentRepository.deploymentDatabase.deleteDeployment(deployment)
        }

        EventBus.fire(DeploymentDeletedEvent(deployment))
    }

}