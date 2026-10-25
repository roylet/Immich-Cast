package com.roylet.immichcast.core

import java.time.LocalDate

/** Date ranges behind each [SlideshowMode] (Random has none: it uses the whole library). */
object Modes {

    /** Days either side of today that count as "on this day". */
    const val ON_THIS_DAY_WINDOW = 3

    /** The season a seasonal slideshow shows today, or null when the mode isn't seasonal. */
    fun season(config: SlideshowConfig, today: LocalDate): Season? = when {
        config.mode != SlideshowMode.SEASONAL -> null
        else -> config.seasonSource.fixedSeason ?: Season.of(today)
    }

    /** The last [days] days, including today. */
    fun recent(today: LocalDate, days: Int): DateRange = DateRange(today.minusDays(days - 1L), today.plusDays(1))

    /**
     * A week centred on today's date in each earlier year in [years]. February 29th maps to
     * the 28th in non-leap years.
     */
    fun onThisDay(today: LocalDate, years: Iterable<Int>, window: Int = ON_THIS_DAY_WINDOW): List<DateRange> =
        years.filter { it < today.year }.distinct().sortedDescending().map { year ->
            val centre = today.withYear(year)
            DateRange(centre.minusDays(window.toLong()), centre.plusDays(window + 1L))
        }

    /** Distinct years among timeline months, newest first. */
    fun yearsWithContent(monthsWithContent: Iterable<LocalDate>): List<Int> =
        monthsWithContent.map { it.year }.distinct().sortedDescending()
}
