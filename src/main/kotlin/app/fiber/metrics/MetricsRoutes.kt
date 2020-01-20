package app.fiber.metrics

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import java.util.*

data class Metrics(val serverId: UUID, val cpuUsage: Double, val memoryUsage: Long, val maxMemory: Long, val time: Long)

fun Route.metrics() = route("/api/metrics"){

    post {
        val metrics = this.call.receive<Metrics>()
        TODO("Not implemented")
        this.call.respond(HttpStatusCode.Created)
    }

    get {
        TODO("Not necessary yet")
    }

}