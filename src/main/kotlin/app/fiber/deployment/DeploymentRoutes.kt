package app.fiber.deployment

import app.fiber.model.Deployment
import app.fiber.model.DeploymentRepository
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Route.deployment() = route("/api/deployment") {

    val deploymentRepository by inject<DeploymentRepository>()

    post {
        val deployment = this.call.receive<Deployment>()
        deploymentRepository.insertDeployment(deployment)

        this.call.respond(HttpStatusCode.Created)
    }

    get("/{id}") {
        val uuid = this.call.parameters["id"]!!
        val deployment = deploymentRepository.getDeploymentById(UUID.fromString(uuid))

        if (deployment == null) {
            this.call.respond(HttpStatusCode.NotFound)
        } else {
            this.call.respond(deployment)
        }
    }

    put {
        val deployment = this.call.receive<Deployment>()
        deploymentRepository.insertDeployment(deployment)

        this.call.respond(HttpStatusCode.OK)
    }

    delete("/{id}") {
        val uuid = this.call.parameters["id"]!!
        val deployment = deploymentRepository.getDeploymentById(UUID.fromString(uuid))

        if (deployment == null) {
            this.call.respond(HttpStatusCode.NotFound)
        } else {
            deploymentRepository.deleteDeployment(deployment)
            this.call.respond(HttpStatusCode.OK)
        }
    }

}
