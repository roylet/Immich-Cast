package com.roylet.immichcast.core

import java.time.LocalDate
import java.time.Month

/** Meteorological seasons for the Southern Hemisphere. */
enum class Season(val months: Set<Month>) {
    SPRING(setOf(Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER)),
    SUMMER(setOf(Month.DECEMBER, Month.JANUARY, Month.FEBRUARY)),
    AUTUMN(setOf(Month.MARCH, Month.APRIL, Month.MAY)),
    WINTER(setOf(Month.JUNE, Month.JULY, Month.AUGUST));

    /** First month of the season; Summer starts in December of its "season year". */
    val firstMonth: Month
        get() = when (this) {
            SPRING -> Month.SEPTEMBER
            SUMMER -> Month.DECEMBER
            AUTUMN -> Month.MARCH
            WINTER -> Month.JUNE
        }

    companion object {
        fun of(date: LocalDate): Season = entries.first { date.month in it.months }
    }
}

/** A half-open date range: [start, endExclusive). */
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive
}

object Seasons {

    /**
     * The calendar year in which the season containing [date] began.
     * January and February belong to the Summer that started the previous December.
     */
    fun seasonYear(date: LocalDate): Int =
        if (Season.of(date) == Season.SUMMER && date.month != Month.DECEMBER) date.year - 1 else date.year

    /** Date range covered by [season] when it starts in [seasonYear]. */
    fun range(season: Season, seasonYear: Int): DateRange {
        val start = LocalDate.of(seasonYear, season.firstMonth, 1)
        return DateRange(start, start.plusMonths(3))
    }

    /**
     * Season years (see [seasonYear]) in which [season] has at least one of the given
     * month-start dates, e.g. the time buckets reported by the Immich timeline.
     */
    fun seasonYearsWithContent(season: Season, monthsWithContent: Iterable<LocalDate>): List<Int> =
        monthsWithContent
            .filter { Season.of(it) == season }
            .map { seasonYear(it) }
            .distinct()
            .sortedDescending()
}
