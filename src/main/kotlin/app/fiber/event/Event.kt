package app.fiber.event

import kotlin.reflect.KClass

interface Event

object EventBus {

    val subscribers = mutableMapOf<KClass<*>, MutableList<(Event) -> Unit>>()

    inline fun <reified T : Event> subscribe(noinline handler: (T) -> Unit) {
        if (this.subscribers[T::class] == null) {
            this.subscribers[T::class] = mutableListOf()
        }

        @Suppress("UNCHECKED_CAST")
        this.subscribers[T::class]!!.add(handler as (Event) -> Unit)
    }

    fun fire(event: Event) = this.subscribers[event::class]?.forEach { it(event) }

}