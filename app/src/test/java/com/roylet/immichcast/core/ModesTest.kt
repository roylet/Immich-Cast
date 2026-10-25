package com.roylet.immichcast.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ModesTest {

    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun `recent covers the last n days including today`() {
        val range = Modes.recent(today, 30)
        assertEquals(LocalDate.of(2026, 9, 26), range.start)
        assertTrue(today in range)
        assertTrue(LocalDate.of(2026, 9, 26) in range)
        assertFalse(LocalDate.of(2026, 9, 25) in range)
        assertFalse(today.plusDays(1) in range)
    }

    @Test
    fun `on this day is a week around today in past years only`() {
        val ranges = Modes.onThisDay(today, listOf(2026, 2024, 2019, 2024))
        assertEquals(listOf(2024, 2019), ranges.map { it.start.year })
        val r2024 = ranges.first()
        assertEquals(LocalDate.of(2024, 10, 22), r2024.start)
        assertEquals(LocalDate.of(2024, 10, 29), r2024.endExclusive)
        assertTrue(LocalDate.of(2024, 10, 28) in r2024)
    }

    @Test
    fun `leap day maps to the 28th in other years`() {
        val ranges = Modes.onThisDay(LocalDate.of(2028, 2, 29), listOf(2027))
        assertTrue(LocalDate.of(2027, 2, 28) in ranges.single())
        assertTrue(LocalDate.of(2027, 3, 3) in ranges.single())
    }

    @Test
    fun `season only applies to the seasonal mode`() {
        val seasonal = SlideshowConfig()
        assertEquals(Season.SPRING, Modes.season(seasonal, today))
        assertEquals(Season.WINTER, Modes.season(seasonal.copy(seasonSource = SeasonSource.WINTER), today))
        assertNull(Modes.season(seasonal.copy(mode = SlideshowMode.RANDOM), today))
        assertNull(Modes.season(seasonal.copy(mode = SlideshowMode.RECENT), today))
    }

    @Test
    fun `random is always shuffled`() {
        val config = SlideshowConfig(mode = SlideshowMode.RANDOM, order = PlaybackOrder.OLDEST_FIRST)
        assertEquals(PlaybackOrder.SHUFFLE, config.effectiveOrder)
        assertEquals(PlaybackOrder.OLDEST_FIRST, config.copy(mode = SlideshowMode.RECENT).effectiveOrder)
    }
}
