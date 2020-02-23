package app.fiber.event

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * EventBus to handle events with coroutines easily.
 *
 * @author x7airworker
 * @author Tammo0987
 * @since 1.0
 */
object EventBus {

    /**
     * Map of [subscribers][EventHandler] to [Event] class as key.
     */
    val subscribers = mutableMapOf<KClass<*>, MutableList<EventHandler>>()

    /**
     * Subscribe to an specific [Event].
     *
     * @param [handler] [EventHandler] which listens on the [Event][T].
     */
    inline fun <reified T : Event> subscribe(noinline handler: suspend (T) -> Unit) {
        if (this.subscribers[T::class] == null) {
            this.subscribers[T::class] = mutableListOf()
        }

        @Suppress("UNCHECKED_CAST")
        this.subscribers[T::class]!!.add(handler as EventHandler)
    }

    /**
     * Fire a specific [event][Event] and launch a coroutine for every [EventHandler].
     *
     * @param [event] [Event] to fire.
     *
     * @return [List] of [jobs][Job] to sync the event.
     */
    fun fire(event: Event) = this.subscribers[event::class]?.map { handler ->
        GlobalScope.launch {
            handler.invoke(event)
        }
    }

}

/**
 * Typealias to improve the readability.
 *
 * @author Tammo0987
 * @since 1.0
 */
typealias EventHandler = suspend (Event) -> Unit