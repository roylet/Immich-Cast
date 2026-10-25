package com.roylet.immichcast.core

import java.time.LocalDate
import java.time.Month

enum class Hemisphere { NORTHERN, SOUTHERN }

/** Meteorological seasons. The same season falls six months apart in the two hemispheres. */
enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    /** First month of the season. The season starting in December runs into the next year. */
    fun firstMonth(hemisphere: Hemisphere): Month {
        val southern = when (this) {
            SPRING -> Month.SEPTEMBER
            SUMMER -> Month.DECEMBER
            AUTUMN -> Month.MARCH
            WINTER -> Month.JUNE
        }
        return if (hemisphere == Hemisphere.SOUTHERN) southern else southern.plus(6)
    }

    fun months(hemisphere: Hemisphere): List<Month> = (0L..2L).map { firstMonth(hemisphere).plus(it) }

    /** True for the season that starts in December: Summer in the south, Winter in the north. */
    fun spansNewYear(hemisphere: Hemisphere): Boolean = firstMonth(hemisphere) == Month.DECEMBER

    companion object {
        fun of(date: LocalDate, hemisphere: Hemisphere): Season = entries.first { date.month in it.months(hemisphere) }
    }
}

/** A half-open date range: [start, endExclusive). */
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive
}

object Seasons {

    /**
     * The calendar year in which the season containing [date] began. January and February
     * belong to the season that started the previous December.
     */
    fun seasonYear(date: LocalDate, hemisphere: Hemisphere): Int =
        if (Season.of(date, hemisphere).spansNewYear(hemisphere) && date.month != Month.DECEMBER) date.year - 1 else date.year

    /** Date range covered by [season] when it starts in [seasonYear]. */
    fun range(season: Season, seasonYear: Int, hemisphere: Hemisphere): DateRange {
        val start = LocalDate.of(seasonYear, season.firstMonth(hemisphere), 1)
        return DateRange(start, start.plusMonths(3))
    }

    /**
     * Season years (see [seasonYear]) in which [season] has at least one of the given
     * month-start dates, e.g. the time buckets reported by the Immich timeline.
     */
    fun seasonYearsWithContent(season: Season, monthsWithContent: Iterable<LocalDate>, hemisphere: Hemisphere): List<Int> =
        monthsWithContent
            .filter { Season.of(it, hemisphere) == season }
            .map { seasonYear(it, hemisphere) }
            .distinct()
            .sortedDescending()
}

object Hemispheres {

    /**
     * Countries (ISO 3166 codes) that lie mostly south of the equator, by where people live.
     * Countries on the equator have little in the way of seasons either way.
     */
    private val SOUTHERN = setOf(
        "AO", "AQ", "AR", "AU", "BI", "BO", "BR", "BV", "BW", "CK", "CL", "FJ", "FK", "GS", "HM",
        "ID", "IO", "KM", "LS", "MG", "MU", "MW", "MZ", "NA", "NC", "NF", "NR", "NU", "NZ", "PE",
        "PF", "PG", "PN", "PY", "RE", "RW", "SB", "SC", "SH", "SZ", "TF", "TK", "TL", "TO", "TV",
        "TZ", "UY", "VU", "WF", "WS", "YT", "ZA", "ZM", "ZW",
    )

    /** The hemisphere of [countryCode], or null when it isn't a known two-letter code. */
    fun forCountry(countryCode: String?): Hemisphere? {
        val code = countryCode?.trim()?.uppercase()?.takeIf { it.length == 2 && it.all(Char::isLetter) } ?: return null
        return if (code in SOUTHERN) Hemisphere.SOUTHERN else Hemisphere.NORTHERN
    }
}
