package com.roylet.immichcast.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeasonTest {

    private val S = Hemisphere.SOUTHERN
    private val N = Hemisphere.NORTHERN

    @Test
    fun `months map to southern hemisphere seasons`() {
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 9, 1), S))
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 11, 30), S))
        assertEquals(Season.SUMMER, Season.of(LocalDate.of(2026, 12, 1), S))
        assertEquals(Season.SUMMER, Season.of(LocalDate.of(2024, 2, 29), S))
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 3, 1), S))
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 5, 31), S))
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 6, 1), S))
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 8, 31), S))
    }

    @Test
    fun `january and february belong to the summer that began the previous december`() {
        assertEquals(2023, Seasons.seasonYear(LocalDate.of(2024, 2, 29), S))
        assertEquals(2023, Seasons.seasonYear(LocalDate.of(2024, 1, 1), S))
        assertEquals(2024, Seasons.seasonYear(LocalDate.of(2024, 12, 31), S))
        assertEquals(2024, Seasons.seasonYear(LocalDate.of(2024, 3, 1), S))
    }

    @Test
    fun `summer range crosses the year boundary and includes leap day`() {
        val range = Seasons.range(Season.SUMMER, 2023, S)
        assertEquals(LocalDate.of(2023, 12, 1), range.start)
        assertEquals(LocalDate.of(2024, 3, 1), range.endExclusive)
        assertTrue(LocalDate.of(2024, 2, 29) in range)
        assertFalse(LocalDate.of(2024, 3, 1) in range)
        assertFalse(LocalDate.of(2023, 11, 30) in range)
    }

    @Test
    fun `each season range covers exactly its three months`() {
        for (season in Season.entries) {
            val range = Seasons.range(season, 2025, S)
            var day = range.start
            while (day < range.endExclusive) {
                assertEquals(season, Season.of(day, S))
                day = day.plusDays(1)
            }
            assertTrue(Season.of(range.start.minusDays(1), S) != season)
            assertTrue(Season.of(range.endExclusive, S) != season)
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
        assertEquals(listOf(2025, 2024, 2023), Seasons.seasonYearsWithContent(Season.SUMMER, months, S))
        assertEquals(listOf(2024), Seasons.seasonYearsWithContent(Season.WINTER, months, S))
        assertEquals(emptyList<Int>(), Seasons.seasonYearsWithContent(Season.SPRING, months, S))
    }

    @Test
    fun `northern seasons are six months on`() {
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 3, 1), N))
        assertEquals(Season.SUMMER, Season.of(LocalDate.of(2026, 7, 4), N))
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 10, 25), N))
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 12, 1), N))
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2027, 2, 28), N))
    }

    @Test
    fun `northern winter crosses the new year instead of summer`() {
        assertTrue(Season.WINTER.spansNewYear(N))
        assertFalse(Season.SUMMER.spansNewYear(N))
        assertEquals(2025, Seasons.seasonYear(LocalDate.of(2026, 1, 15), N))
        assertEquals(2026, Seasons.seasonYear(LocalDate.of(2026, 7, 15), N))
        val winter = Seasons.range(Season.WINTER, 2025, N)
        assertEquals(LocalDate.of(2025, 12, 1), winter.start)
        assertEquals(LocalDate.of(2026, 3, 1), winter.endExclusive)
        assertEquals(listOf(2025, 2024), Seasons.seasonYearsWithContent(Season.WINTER, listOf(LocalDate.of(2026, 2, 1), LocalDate.of(2024, 12, 1)), N))
    }

    @Test
    fun `hemisphere comes from the country`() {
        assertEquals(Hemisphere.SOUTHERN, Hemispheres.forCountry("AU"))
        assertEquals(Hemisphere.SOUTHERN, Hemispheres.forCountry("nz"))
        assertEquals(Hemisphere.SOUTHERN, Hemispheres.forCountry("BR"))
        assertEquals(Hemisphere.NORTHERN, Hemispheres.forCountry("GB"))
        assertEquals(Hemisphere.NORTHERN, Hemispheres.forCountry("US"))
        assertEquals(null, Hemispheres.forCountry("001")) // ICU's code for "no country", e.g. UTC
        assertEquals(null, Hemispheres.forCountry(""))
        assertEquals(null, Hemispheres.forCountry(null))
    }
}
