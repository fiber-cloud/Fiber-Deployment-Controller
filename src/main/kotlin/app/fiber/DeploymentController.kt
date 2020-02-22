package app.fiber

import app.fiber.deployment.DeploymentRepository
import app.fiber.deployment.database.CassandraDeploymentDatabase
import app.fiber.deployment.database.DeploymentDatabase
import app.fiber.deployment.route.deployment
import app.fiber.deployment.service.DeploymentService
import app.fiber.deployment.service.kubernetes.KubernetesDeploymentService
import app.fiber.image.ImageAllocatorService
import app.fiber.image.TemplateImageAllocatorService
import app.fiber.logger.logger
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
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
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    startKoin { modules(deploymentControllerModule) }
    embeddedServer(Netty, commandLineEnvironment(args)).start(true)
}

val deploymentControllerModule = module {
    val logger by logger()

    val cassandraHost = System.getenv("CASSANDRA_SERVICE_HOST") ?: "".also {
        logger.error("Cassandra host not found!")
    }

    val session: CqlSession = CqlSessionBuilder()
            .addContactPoint(InetSocketAddress(cassandraHost, 9042))
            .withLocalDatacenter("datacenter1")
            .build()

    val templateStorageHost = System.getenv("FIBER_TEMPLATE_STORAGE_SERVICE_HOST") ?: "".also {
        logger.error("Fiber-Template-Storage host not found!")
    }

    val httpClient = HttpClient {
        install(JsonFeature)
    }
    Runtime.getRuntime().addShutdownHook(Thread(httpClient::close))

    single { httpClient }

    single { DefaultKubernetesClient() }
    single<ImageAllocatorService> { TemplateImageAllocatorService(templateStorageHost) }

    single<DeploymentDatabase> { CassandraDeploymentDatabase(session) }
    single<DeploymentService> { KubernetesDeploymentService() }
    single { DeploymentRepository() }
}

fun Application.main() {
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
