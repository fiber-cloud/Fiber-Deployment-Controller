package app.fiber.model

import java.util.*

data class Deployment(
        val uuid: UUID,
        val name: String,
        val template: String,
        val minimumAmount: Int,
        val maximumAmount: Int,
        val jvmConfiguration: List<String>,
        val startParameters: List<String>,
        val systemProperties: Map<String, String>,
        val environment: Map<String, String>
)