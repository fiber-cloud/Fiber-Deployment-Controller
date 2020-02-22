package app.fiber.image

import app.fiber.deployment.model.Deployment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.*
import kotlin.test.assertEquals

class TemplateImageAllocatorServiceTest : KoinTest {

    private val uuid = UUID.randomUUID()

    private val imageAllocatorService by inject<ImageAllocatorService>()

    @Before
    fun setUp() {
        val client = HttpClient(MockEngine) {
            install(JsonFeature)

            engine {
                addHandler { request ->
                    when (request.url.toString()) {
                        "http://test.com/template/api/snapshots/$uuid" -> {

                            respond(
                                    json { "image" to "ResponseImage" }.toString(),
                                    headers = headersOf(
                                            HttpHeaders.ContentType,
                                            ContentType.Application.Json.toString()
                                    )
                            )
                        }
                        else -> error("Unhandled url ${request.url}")
                    }
                }
            }
        }

        startKoin {}

        loadKoinModules(module {
            single { client }
            single<ImageAllocatorService> { TemplateImageAllocatorService("test.com") }
        })
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `test get image if deployment is dynamic`() {
        val deployment = this.testDeployment(true)
        runBlocking {
            val image = imageAllocatorService.getCurrentImage(deployment)
            assertEquals("Image", image)
        }
    }

    @Test
    fun `test get image if deployment is static`() {
        val deployment = this.testDeployment(false)
        runBlocking {
            val image = imageAllocatorService.getCurrentImage(deployment)
            assertEquals("ResponseImage", image)
        }
    }

    private fun testDeployment(dynamic: Boolean): Deployment {
        return Deployment(
                this.uuid,
                "Test",
                "Image",
                "Server",
                dynamic,
                0,
                0,
                emptyMap()
        )
    }

}