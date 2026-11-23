package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.ui.HistoryTimeBucket
import com.cnsoftware.waveform.shared.ui.calculateListeningStreakDays
import com.cnsoftware.waveform.shared.ui.episodeMetaText
import com.cnsoftware.waveform.shared.ui.episodeProgress
import com.cnsoftware.waveform.shared.ui.formatDurationShort
import com.cnsoftware.waveform.shared.ui.formatEpisodeDate
import com.cnsoftware.waveform.shared.ui.formatHistoryTimestamp
import com.cnsoftware.waveform.shared.ui.formatListeningSeconds
import com.cnsoftware.waveform.shared.ui.historyTimeBucket
import com.cnsoftware.waveform.shared.ui.lastSevenDaysActivity
import com.cnsoftware.waveform.shared.ui.parseDurationMs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EpisodeFormatTest {
    // 2026-11-16 12:00 UTC
    private val now = 1_794_830_400_000L

    @Test
    fun relativeDates() {
        assertEquals("Bugün", formatEpisodeDate("Mon, 16 Nov 2026 03:00:00 +0000", now))
        assertEquals("Dün", formatEpisodeDate("Sun, 15 Nov 2026 23:30:00 +0000", now))
        assertEquals("2 gün önce", formatEpisodeDate("Sat, 14 Nov 2026 06:01:00 GMT", now))
        assertEquals("6 gün önce", formatEpisodeDate("Tue, 10 Nov 2026 06:01:00 +0300", now))
    }

    @Test
    fun absoluteDatesDropYearOnlyForCurrentYear() {
        assertEquals("7 Kas", formatEpisodeDate("Sat, 07 Nov 2026 06:01:00 +0000", now))
        assertEquals("26 Tem 2023", formatEpisodeDate("Wed, 26 Jul 2023 18:00:00 +0000", now))
        assertEquals("1 Oca", formatEpisodeDate("1 Jan 2026", now))
    }

    @Test
    fun unparsableDateIsKeptAndBlankIsEmpty() {
        assertEquals("geçen salı", formatEpisodeDate("geçen salı", now))
        assertEquals("", formatEpisodeDate("  ", now))
    }

    @Test
    fun durationParsing() {
        assertEquals(3_723_000L, parseDurationMs("1:02:03"))
        assertEquals(2_530_000L, parseDurationMs("42:10"))
        assertEquals(2_520_000L, parseDurationMs("2520"))
        assertNull(parseDurationMs(""))
        assertNull(parseDurationMs("abc"))
        assertNull(parseDurationMs("0"))
    }

    @Test
    fun durationFormatting() {
        assertEquals("42 dk", formatDurationShort(2_530_000L))
        assertEquals("1 sa", formatDurationShort(3_600_000L))
        assertEquals("1 sa 5 dk", formatDurationShort(3_900_000L))
        assertEquals("1 dk", formatDurationShort(10_000L))
    }

    @Test
    fun metaText() {
        val date = "Sun, 15 Nov 2026 08:00:00 +0000"
        assertEquals("Dün • 42 dk", episodeMetaText(date, "42:00", 0L, false, now))
        assertEquals("Dün • 32 dk kaldı", episodeMetaText(date, "42:00", 600_000L, false, now))
        assertEquals("Dinlendi • Dün • 42 dk", episodeMetaText(date, "42:00", 600_000L, true, now))
        assertEquals("Dün • Kalınan: 10:00", episodeMetaText(date, "", 600_000L, false, now))
        assertEquals("Dün", episodeMetaText(date, "", 0L, false, now))
    }

    @Test
    fun progress() {
        assertEquals(0.25f, episodeProgress("40:00", 600_000L))
        assertNull(episodeProgress("40:00", 0L))
        assertNull(episodeProgress("", 600_000L))
        assertEquals(1f, episodeProgress("10:00", 900_000L))
    }

    @Test
    fun respectsLocalTimezoneOffsetAcrossMidnight() {
        // 2026-11-15 22:00 UTC == 2026-11-16 01:00 +03:00
        val now22Utc = 1_794_780_000_000L
        val istanbulOffsetMs = 3 * 3_600_000L

        // 21:30 UTC == 16 Kas 00:30 +03:00 -> Bugun
        assertEquals("Bugün", formatEpisodeDate("Sun, 15 Nov 2026 21:30:00 +0000", now22Utc, istanbulOffsetMs))
        // 20:00 UTC == 15 Kas 23:00 +03:00 -> Yerel saatte Dun (UTC'de Bugun olurdu)
        assertEquals("Dün", formatEpisodeDate("Sun, 15 Nov 2026 20:00:00 +0000", now22Utc, istanbulOffsetMs))
        assertEquals("Bugün", formatEpisodeDate("Sun, 15 Nov 2026 20:00:00 +0000", now22Utc, 0L))
    }

    @Test
    fun historyBucketsAndTimestampsRespectLocalTime() {
        val dayMs = 86_400_000L
        assertEquals(HistoryTimeBucket.OLDER, historyTimeBucket(0L, now, 0L))
        assertEquals("Önceki kayıt", formatHistoryTimestamp(0L, now, 0L))

        assertEquals(HistoryTimeBucket.TODAY, historyTimeBucket(now - 300_000L, now, 0L))
        assertEquals("Bugün, 11:55", formatHistoryTimestamp(now - 300_000L, now, 0L))

        assertEquals(HistoryTimeBucket.YESTERDAY, historyTimeBucket(now - dayMs, now, 0L))
        assertEquals("Dün, 12:00", formatHistoryTimestamp(now - dayMs, now, 0L))

        assertEquals(HistoryTimeBucket.THIS_WEEK, historyTimeBucket(now - 3 * dayMs, now, 0L))
        assertEquals("3 gün önce, 12:00", formatHistoryTimestamp(now - 3 * dayMs, now, 0L))

        assertEquals(HistoryTimeBucket.OLDER, historyTimeBucket(now - 10 * dayMs, now, 0L))
        assertEquals("6 Kas, 12:00", formatHistoryTimestamp(now - 10 * dayMs, now, 0L))
        assertEquals("16 Kas 2025, 12:00", formatHistoryTimestamp(now - 365 * dayMs, now, 0L))
    }

    @Test
    fun sevenDayActivityAndStreakCalculateCorrectly() {
        val bars = lastSevenDaysActivity(
            dailyListeningSeconds = mapOf("2026-11-16" to 600L, "2026-11-14" to 180L),
            nowMs = now,
            utcOffsetMs = 0L
        )
        assertEquals(7, bars.size)
        assertEquals(listOf("Sal", "Çar", "Per", "Cum", "Cmt", "Paz", "Pzt"), bars.map { it.shortDayLabel })
        assertEquals("2026-11-16", bars.single { it.isToday }.dayKey)
        assertEquals(600L, bars.last().seconds)
        assertEquals(180L, bars.first { it.dayKey == "2026-11-14" }.seconds)

        assertEquals(3, calculateListeningStreakDays(setOf("2026-11-14", "2026-11-15", "2026-11-16"), now, 0L))
        assertEquals(2, calculateListeningStreakDays(setOf("2026-11-14", "2026-11-15"), now, 0L))
        assertEquals(1, calculateListeningStreakDays(setOf("2026-11-14", "2026-11-16"), now, 0L))
        assertEquals(0, calculateListeningStreakDays(setOf("2026-11-14"), now, 0L))
    }

    @Test
    fun formatListeningSecondsHandlesAllRanges() {
        assertEquals("0 dk", formatListeningSeconds(0L))
        assertEquals("45 sn", formatListeningSeconds(45L))
        assertEquals("2 dk", formatListeningSeconds(125L))
        assertEquals("1 sa", formatListeningSeconds(3_600L))
        assertEquals("1 sa 5 dk", formatListeningSeconds(3_900L))
    }
}
