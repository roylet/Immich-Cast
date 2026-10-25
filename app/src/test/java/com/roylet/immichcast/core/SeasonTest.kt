package com.roylet.immichcast.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeasonTest {

    @Test
    fun `months map to southern hemisphere seasons`() {
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 9, 1)))
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 11, 30)))
        assertEquals(Season.SUMMER, Season.of(LocalDate.of(2026, 12, 1)))
        assertEquals(Season.SUMMER, Season.of(LocalDate.of(2024, 2, 29)))
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 3, 1)))
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 5, 31)))
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 6, 1)))
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 8, 31)))
    }

    @Test
    fun `january and february belong to the summer that began the previous december`() {
        assertEquals(2023, Seasons.seasonYear(LocalDate.of(2024, 2, 29)))
        assertEquals(2023, Seasons.seasonYear(LocalDate.of(2024, 1, 1)))
        assertEquals(2024, Seasons.seasonYear(LocalDate.of(2024, 12, 31)))
        assertEquals(2024, Seasons.seasonYear(LocalDate.of(2024, 3, 1)))
    }

    @Test
    fun `summer range crosses the year boundary and includes leap day`() {
        val range = Seasons.range(Season.SUMMER, 2023)
        assertEquals(LocalDate.of(2023, 12, 1), range.start)
        assertEquals(LocalDate.of(2024, 3, 1), range.endExclusive)
        assertTrue(LocalDate.of(2024, 2, 29) in range)
        assertFalse(LocalDate.of(2024, 3, 1) in range)
        assertFalse(LocalDate.of(2023, 11, 30) in range)
    }

    @Test
    fun `each season range covers exactly its three months`() {
        for (season in Season.entries) {
            val range = Seasons.range(season, 2025)
            var day = range.start
            while (day < range.endExclusive) {
                assertEquals(season, Season.of(day))
                day = day.plusDays(1)
            }
            assertTrue(Season.of(range.start.minusDays(1)) != season)
            assertTrue(Season.of(range.endExclusive) != season)
        }
    }

    @Test
    fun `season years are derived from timeline months`() {
        val months = listOf(
            LocalDate.of(2025, 12, 1), // summer 2025
            LocalDate.of(2025, 1, 1), // summer 2024
            LocalDate.of(2024, 2, 1), // summer 2023
            LocalDate.of(2024, 1, 1), // summer 2023 again
            LocalDate.of(2024, 7, 1), // winter: ignored
        )
        assertEquals(listOf(2025, 2024, 2023), Seasons.seasonYearsWithContent(Season.SUMMER, months))
        assertEquals(listOf(2024), Seasons.seasonYearsWithContent(Season.WINTER, months))
        assertEquals(emptyList<Int>(), Seasons.seasonYearsWithContent(Season.SPRING, months))
    }
}
