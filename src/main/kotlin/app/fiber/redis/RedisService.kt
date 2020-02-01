package app.fiber.redis

import io.lettuce.core.RedisClient

class RedisService(ip: String) : KeyValueStore {

    private val client = RedisClient.create("redis://$ip")

    private val connection = client.connect()

    private val commands = connection.sync()

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this::close))
    }

    override fun get(key: String): String = this.commands.get(key)

    override fun set(key: String, value: String, expires: Long) {
        this.commands.set(key, value)

        if (expires >= 0) { this.commands.expire(key, expires) }
    }

    override fun delete(key: String) {
        this.commands.del(key)
    }

    private fun close() {
        this.connection.close()
        this.client.shutdown()
    }

}