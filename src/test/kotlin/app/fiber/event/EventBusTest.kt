package app.fiber.event

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class EventBusTest {

    @After
    fun tearDown() = EventBus.subscribers.clear()

    @Test
    fun `test if even bus fires the events`() {
        val count = AtomicInteger(0)

        val eventHandler: suspend (TestEvent) -> Unit = { event -> count.getAndAdd(event.add) }

        EventBus.subscribe(eventHandler)
        EventBus.subscribe(eventHandler)

        runBlocking {
            EventBus.fire(TestEvent(1))!!.joinAll()
        }

        assertEquals(2, count.get())
    }

    inner class TestEvent(val add: Int) : Event

}