package com.example.magicapp

import com.example.magicapp.features.compass.CompassRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class CompassCardinalTest {

    // toCardinal is a companion object function — no Android context required.
    // Call it directly via CompassRepository.toCardinal(deg).

    @Test fun north() = assertEquals("N", CompassRepository.toCardinal(0f))
    @Test fun northAlsoAtMax() = assertEquals("N", CompassRepository.toCardinal(359f))
    @Test fun northEast() = assertEquals("NE", CompassRepository.toCardinal(45f))
    @Test fun east() = assertEquals("E", CompassRepository.toCardinal(90f))
    @Test fun southEast() = assertEquals("SE", CompassRepository.toCardinal(135f))
    @Test fun south() = assertEquals("S", CompassRepository.toCardinal(180f))
    @Test fun southWest() = assertEquals("SW", CompassRepository.toCardinal(225f))
    @Test fun west() = assertEquals("W", CompassRepository.toCardinal(270f))
    @Test fun northWest() = assertEquals("NW", CompassRepository.toCardinal(315f))
}

// --- Trip Timer Tests ---

class ClockRepositoryTripTest {

    @Test
    fun `startTrip sets tripRunning true`() {
        val repo = com.example.magicapp.features.clock.ClockRepository()
        repo.startTrip()
        assertEquals(true, repo.state.value.tripRunning)
    }

    @Test
    fun `stopTrip sets tripRunning false`() {
        val repo = com.example.magicapp.features.clock.ClockRepository()
        repo.startTrip()
        repo.stopTrip()
        assertEquals(false, repo.state.value.tripRunning)
    }

    @Test
    fun `resetTrip sets elapsed to zero and tripRunning false`() {
        val repo = com.example.magicapp.features.clock.ClockRepository()
        repo.startTrip()
        repo.resetTrip()
        assertEquals(0L, repo.state.value.tripElapsedSec)
        assertEquals(false, repo.state.value.tripRunning)
    }
}

// --- CarButtonRepository Tests ---
// Uses the (Long, Int, Int) overload to avoid instantiating android.view.KeyEvent
// in a JVM test environment (Android stubs throw RuntimeException when called from JVM).

class CarButtonRepositoryTest {

    // KeyEvent constants are safe to reference as they are just Int literals.
    private val ACTION_DOWN = android.view.KeyEvent.ACTION_DOWN
    private val ACTION_UP = android.view.KeyEvent.ACTION_UP
    private val KEYCODE_MEDIA_PLAY = android.view.KeyEvent.KEYCODE_MEDIA_PLAY

    @Test
    fun `onKeyEvent appends event to list`() {
        val repo = com.example.magicapp.features.events.CarButtonRepository()
        repo.onKeyEvent(System.currentTimeMillis(), ACTION_DOWN, KEYCODE_MEDIA_PLAY)
        assertEquals(1, repo.events.value.size)
        assertEquals("DOWN", repo.events.value[0].action)
    }

    @Test
    fun `onKeyEvent records UP action correctly`() {
        val repo = com.example.magicapp.features.events.CarButtonRepository()
        repo.onKeyEvent(System.currentTimeMillis(), ACTION_UP, KEYCODE_MEDIA_PLAY)
        assertEquals("UP", repo.events.value[0].action)
    }

    @Test
    fun `onKeyEvent keeps only last 100 events`() {
        val repo = com.example.magicapp.features.events.CarButtonRepository()
        repeat(105) {
            repo.onKeyEvent(System.currentTimeMillis(), ACTION_DOWN, KEYCODE_MEDIA_PLAY)
        }
        assertEquals(100, repo.events.value.size)
    }

    @Test
    fun `clear empties the event list`() {
        val repo = com.example.magicapp.features.events.CarButtonRepository()
        repo.onKeyEvent(System.currentTimeMillis(), ACTION_DOWN, KEYCODE_MEDIA_PLAY)
        repo.clear()
        assertEquals(0, repo.events.value.size)
    }
}
