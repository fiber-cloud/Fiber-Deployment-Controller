package app.fiber.model

import java.util.*

data class Deployment(
        val uuid: UUID,
        val name: String,
        val image: String,
        val type: String,
        val minimumAmount: Int,
        val maximumAmount: Int,
        val environment: Map<String, String>
)