package app.fiber

import app.fiber.cassandra.CassandraConnector
import app.fiber.deployment.deployment
import app.fiber.model.DeploymentRepository
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import java.io.PrintWriter
import java.io.StringWriter

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(true)
}

fun Application.main() {
    install(Koin) {
        modules(deploymentControllerModule)
    }

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        gson()
    }

    install(StatusPages) {
        exception<Exception> {
            val writer = StringWriter()
            it.printStackTrace(PrintWriter(writer))

            this.call.respond(
                    TextContent(
                            writer.toString(),
                            ContentType.Text.Plain.withCharset(Charsets.UTF_8),
                            HttpStatusCode.InternalServerError
                    )
            )
        }
    }

    install(Routing) {
        deployment()
    }
}

val deploymentControllerModule = module {
    val cassandraHost = System.getenv("CASSANDRA_SERVICE_HOST") ?: throw Exception("Cassandra host not found!")

    val cassandra = CassandraConnector(cassandraHost)
    val deploymentRepository = DeploymentRepository(cassandra.session)

    single { deploymentRepository }
}