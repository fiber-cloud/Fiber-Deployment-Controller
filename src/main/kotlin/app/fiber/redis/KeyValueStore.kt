package app.fiber.redis

interface KeyValueStore {

    fun get(key: String): String

    fun set(key: String, value: String, expires: Long = -1)

    fun delete(key: String)

}