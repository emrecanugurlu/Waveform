package com.cnsoftware.waveform.shared.ui

import kotlin.time.Clock

/** Bölüm satırlarındaki tarih / süre / ilerleme metinleri için saf (test edilebilir) yardımcılar. */

private const val DAY_MS = 86_400_000L

private val MONTHS_EN = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
private val MONTHS_TR = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
private val DAYS_TR = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

internal enum class HistoryTimeBucket {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    OLDER
}

internal data class DailyListeningBar(
    val dayKey: String,
    val shortDayLabel: String,
    val seconds: Long,
    val isToday: Boolean
)

private val RFC822 = Regex(
    """(?:[A-Za-z]{3,9},?\s*)?(\d{1,2})\s+([A-Za-z]{3})[A-Za-z]*\.?\s+(\d{2,4})""" +
        """(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?\s*([+-]\d{4}|[A-Za-z]{1,5})?"""
)

internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

internal fun localDayKey(
    epochMs: Long,
    utcOffsetMs: Long = localUtcOffsetMillis(epochMs)
): String = formatDayKey((epochMs + utcOffsetMs).floorDiv(DAY_MS))

private fun formatDayKey(day: Long): String {
    val civil = civilFromDays(day)
    val y = civil.year.toString().padStart(4, '0')
    val m = civil.month.toString().padStart(2, '0')
    val d = civil.day.toString().padStart(2, '0')
    return "$y-$m-$d"
}

internal fun historyTimeBucket(
    playedAtMs: Long,
    nowMs: Long,
    utcOffsetMs: Long = localUtcOffsetMillis(nowMs)
): HistoryTimeBucket {
    if (playedAtMs <= 0L) return HistoryTimeBucket.OLDER
    val nowLocalDay = (nowMs + utcOffsetMs).floorDiv(DAY_MS)
    val playedLocalDay = (playedAtMs + utcOffsetMs).floorDiv(DAY_MS)
    val diff = nowLocalDay - playedLocalDay
    return when {
        diff <= 0L -> HistoryTimeBucket.TODAY
        diff == 1L -> HistoryTimeBucket.YESTERDAY
        diff in 2L..6L -> HistoryTimeBucket.THIS_WEEK
        else -> HistoryTimeBucket.OLDER
    }
}

internal fun formatHistoryTimestamp(
    playedAtMs: Long,
    nowMs: Long,
    utcOffsetMs: Long = localUtcOffsetMillis(nowMs)
): String {
    if (playedAtMs <= 0L) return "Önceki kayıt"
    val nowLocalDay = (nowMs + utcOffsetMs).floorDiv(DAY_MS)
    val playedLocalDay = (playedAtMs + utcOffsetMs).floorDiv(DAY_MS)
    val diff = nowLocalDay - playedLocalDay
    val timeOfDaySec = (playedAtMs + utcOffsetMs).mod(DAY_MS) / 1000L
    val hh = (timeOfDaySec / 3600L).toString().padStart(2, '0')
    val mm = ((timeOfDaySec % 3600L) / 60L).toString().padStart(2, '0')
    val clockLabel = "$hh:$mm"
    return when {
        diff <= 0L -> "Bugün, $clockLabel"
        diff == 1L -> "Dün, $clockLabel"
        diff in 2L..6L -> "$diff gün önce, $clockLabel"
        else -> {
            val civil = civilFromDays(playedLocalDay)
            val nowCivil = civilFromDays(nowLocalDay)
            val datePart = if (civil.year == nowCivil.year) {
                "${civil.day} ${MONTHS_TR[civil.month - 1]}"
            } else {
                "${civil.day} ${MONTHS_TR[civil.month - 1]} ${civil.year}"
            }
            "$datePart, $clockLabel"
        }
    }
}

internal fun lastSevenDaysActivity(
    dailyListeningSeconds: Map<String, Long>,
    nowMs: Long,
    utcOffsetMs: Long = localUtcOffsetMillis(nowMs)
): List<DailyListeningBar> {
    val todayDay = (nowMs + utcOffsetMs).floorDiv(DAY_MS)
    return ((todayDay - 6)..todayDay).map { day ->
        val dayKey = formatDayKey(day)
        val weekdayIndex = (day + 3L).mod(7)
        DailyListeningBar(
            dayKey = dayKey,
            shortDayLabel = DAYS_TR[weekdayIndex],
            seconds = (dailyListeningSeconds[dayKey] ?: 0L).coerceAtLeast(0L),
            isToday = day == todayDay
        )
    }
}

internal fun calculateListeningStreakDays(
    activeDayKeys: Set<String>,
    nowMs: Long,
    utcOffsetMs: Long = localUtcOffsetMillis(nowMs)
): Int {
    if (activeDayKeys.isEmpty()) return 0
    val todayDay = (nowMs + utcOffsetMs).floorDiv(DAY_MS)
    val startDay = when {
        formatDayKey(todayDay) in activeDayKeys -> todayDay
        formatDayKey(todayDay - 1) in activeDayKeys -> todayDay - 1
        else -> return 0
    }
    var streak = 0
    var cursor = startDay
    while (formatDayKey(cursor) in activeDayKeys) {
        streak++
        cursor--
    }
    return streak
}

internal fun formatListeningSeconds(seconds: Long): String {
    if (seconds <= 0L) return "0 dk"
    if (seconds < 60L) return "$seconds sn"
    val totalMinutes = seconds / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours == 0L -> "$minutes dk"
        minutes == 0L -> "$hours sa"
        else -> "$hours sa $minutes dk"
    }
}

private data class CivilDate(val year: Int, val month: Int, val day: Int)

private class ParsedDate(val epochMs: Long)

private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = (y).floorDiv(400L)
    val yoe = y - era * 400
    val mp = if (month > 2) month - 3 else month + 9
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

private fun civilFromDays(days: Long): CivilDate {
    val z = days + 719468
    val era = z.floorDiv(146097L)
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
    return CivilDate(year, month, day)
}

private fun parseDate(text: String): ParsedDate? {
    val match = RFC822.find(text.trim()) ?: return null
    val g = match.groupValues
    val day = g[1].toIntOrNull() ?: return null
    val month = MONTHS_EN.indexOf(g[2].lowercase()) + 1
    if (month == 0 || day !in 1..31) return null
    var year = g[3].toIntOrNull() ?: return null
    if (g[3].length == 2) year += if (year < 70) 2000 else 1900
    val hour = g[4].toIntOrNull() ?: 0
    val minute = g[5].toIntOrNull() ?: 0
    val second = g[6].toIntOrNull() ?: 0
    val zone = g[7]
    val offsetMin = if (zone.length == 5 && (zone[0] == '+' || zone[0] == '-')) {
        val sign = if (zone[0] == '-') -1 else 1
        sign * ((zone.substring(1, 3).toIntOrNull() ?: 0) * 60 + (zone.substring(3, 5).toIntOrNull() ?: 0))
    } else {
        0
    }
    val seconds = ((daysFromCivil(year, month, day) * 24 + hour) * 60 + minute) * 60 + second
    return ParsedDate(seconds * 1000 - offsetMin * 60_000L)
}

/**
 * RSS tarihini kısa Türkçe metne çevirir: "Bugün", "Dün", "3 gün önce", "14 Kas", "14 Kas 2024".
 * Çözümlenemezse özgün metni döndürür.
 */
internal fun formatEpisodeDate(pubDate: String, nowMs: Long, utcOffsetMs: Long = 0L): String {
    if (pubDate.isBlank()) return ""
    val parsed = parseDate(pubDate) ?: return pubDate.trim()
    val nowLocalDay = (nowMs + utcOffsetMs).floorDiv(DAY_MS)
    val pubLocalDay = (parsed.epochMs + utcOffsetMs).floorDiv(DAY_MS)
    val diff = nowLocalDay - pubLocalDay
    return when {
        diff == 0L -> "Bugün"
        diff == 1L -> "Dün"
        diff in 2..6 -> "$diff gün önce"
        else -> {
            val pubCivil = civilFromDays(pubLocalDay)
            val base = "${pubCivil.day} ${MONTHS_TR[pubCivil.month - 1]}"
            if (pubCivil.year == civilFromDays(nowLocalDay).year) base else "$base ${pubCivil.year}"
        }
    }
}

/** "1:02:03", "42:10" ya da "2520" (saniye) biçimlerini milisaniyeye çevirir. */
internal fun parseDurationMs(text: String): Long? {
    val parts = text.trim().split(":")
    if (parts.isEmpty() || parts.size > 3) return null
    val numbers = parts.map { it.trim().toLongOrNull() ?: return null }
    if (numbers.any { it < 0 }) return null
    val seconds = when (numbers.size) {
        1 -> numbers[0]
        2 -> numbers[0] * 60 + numbers[1]
        else -> numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
    }
    return seconds.takeIf { it > 0 }?.times(1000)
}

/** "42 dk", "1 sa", "1 sa 5 dk". */
internal fun formatDurationShort(ms: Long): String {
    val totalMin = ((ms + 30_000) / 60_000).coerceAtLeast(1)
    val hours = totalMin / 60
    val minutes = totalMin % 60
    return when {
        hours == 0L -> "$totalMin dk"
        minutes == 0L -> "$hours sa"
        else -> "$hours sa $minutes dk"
    }
}

private fun clock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    fun two(v: Long) = v.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(s)}" else "${two(m)}:${two(s)}"
}

/** Satırın alt metni: "Dün • 42 dk" ya da kısmen dinlenmişse "Dün • 18 dk kaldı". */
internal fun episodeMetaText(
    pubDate: String,
    durationText: String,
    savedMs: Long,
    isPlayed: Boolean,
    nowMs: Long,
    utcOffsetMs: Long = 0L
): String {
    val durationMs = parseDurationMs(durationText)
    val durationPart = when {
        durationMs != null && savedMs > 0L && savedMs < durationMs && !isPlayed ->
            "${formatDurationShort(durationMs - savedMs)} kaldı"
        durationMs != null -> formatDurationShort(durationMs)
        savedMs > 0L && !isPlayed -> "Kalınan: ${clock(savedMs)}"
        else -> durationText.trim()
    }
    return listOf(
        if (isPlayed) "Dinlendi" else "",
        formatEpisodeDate(pubDate, nowMs, utcOffsetMs),
        durationPart
    ).filter { it.isNotBlank() }.joinToString(" • ")
}

/** Kısmen dinlenmiş bölümün ilerlemesi (0..1); süre ya da konum bilinmiyorsa null. */
internal fun episodeProgress(durationText: String, savedMs: Long): Float? {
    val durationMs = parseDurationMs(durationText) ?: return null
    if (savedMs <= 0L) return null
    return (savedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}
