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
