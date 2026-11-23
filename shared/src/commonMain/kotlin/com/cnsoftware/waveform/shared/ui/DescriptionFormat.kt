package com.cnsoftware.waveform.shared.ui

/** Bölüm açıklamasını okunaklı göstermek için: paragraflar + (varsa) zaman damgalı bölüm içeriği. */
internal class Chapter(val timeMs: Long, val label: String)

internal class EpisodeDescription(
    val paragraphs: List<String>,
    val chapters: List<Chapter>
)

private val TIMESTAMP = Regex("""\(?\b(\d{1,2}:\d{2}(?::\d{2})?)\b\)?""")
private val TRAILING_INTRO_LABEL = Regex(
    """(?i)\s*(bu bölümde|bölüm içeriği|içindekiler|zaman damgaları|zamanlar|bölümler)\s*:?\s*$"""
)
private val CREDITS = Regex("""(Sunan|Yapımcı|Hazırlayan|Konuklar|Konuk|Tüm bölümler)(?=\s*[:,]|\s|$)""")
private const val MAX_CHAPTER_LABEL = 90
private const val PARAGRAPH_TARGET_CHARS = 220
private const val PARAGRAPH_MAX_SENTENCES = 3

internal fun parseEpisodeDescription(text: String): EpisodeDescription {
    val clean = text.trim()
    if (clean.isEmpty()) return EpisodeDescription(emptyList(), emptyList())

    val matches = TIMESTAMP.findAll(clean).toList()
    val times = matches.map { parseClockMs(it.groupValues[1]) }
    val ascending = times.none { it == null } &&
        times.zipWithNext().all { (a, b) -> a!! <= b!! }
    if (matches.size < 2 || !ascending) {
        return EpisodeDescription(toParagraphs(clean), emptyList())
    }

    val intro = clean.substring(0, matches.first().range.first).replace(TRAILING_INTRO_LABEL, "")
    val chapters = mutableListOf<Chapter>()
    var trailing = ""
    matches.forEachIndexed { index, match ->
        val end = if (index + 1 < matches.size) matches[index + 1].range.first else clean.length
        var label = tidyLabel(clean.substring(match.range.last + 1, end))
        if (index == matches.lastIndex) {
            // Son maddeden sonra gelen düz yazı (künye, reklam notu vb.) bölüm adı sayılmaz.
            val creditsAt = CREDITS.find(label)?.range?.first?.takeIf { it > 0 }
            val sentenceEnd = if (label.length > MAX_CHAPTER_LABEL) firstSentenceEnd(label) else null
            val cut = listOfNotNull(creditsAt, sentenceEnd).minOrNull()
            if (cut != null && cut < label.length) {
                trailing = label.substring(cut).trim()
                label = label.substring(0, cut).trim()
            }
        }
        if (label.isNotBlank() && (index == matches.lastIndex || label.length <= 2 * MAX_CHAPTER_LABEL)) {
            chapters += Chapter(times[index]!!, label)
        }
    }
    if (chapters.size < 2) {
        return EpisodeDescription(toParagraphs(clean), emptyList())
    }
    return EpisodeDescription(toParagraphs(intro) + toParagraphs(splitCredits(trailing)), chapters)
}

/** "Sunan: X Yapımcı: Y Tüm bölümler…" gibi künye metnini satırlara böler. */
private fun splitCredits(text: String): String =
    text.replace(Regex("""\s*(?=(Sunan|Yapımcı|Hazırlayan|Konuklar|Konuk)\s*:|Tüm bölümler)"""), "\n")
        .trim()

private fun tidyLabel(raw: String): String =
    raw.replace('\n', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimStart('-', '–', '—', ':', '•', '·', ')', ' ')
        .trimEnd('-', '–', '—', ':', ';', ',', ' ')

/** İlk cümlenin bittiği (noktalamadan sonraki) indeks; yoksa null. */
private fun firstSentenceEnd(text: String): Int? {
    for (i in text.indices) {
        if (text[i] in ".!?…" && (i + 1 == text.length || text[i + 1] == ' ')) return i + 1
    }
    return null
}

/** Satır sonları varsa onlara göre; yoksa (düz metin) cümleleri 2–3'lü gruplara bölerek paragraflar üretir. */
internal fun toParagraphs(text: String): List<String> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return emptyList()
    if (trimmed.contains('\n')) {
        return trimmed.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }
    val sentences = splitSentences(trimmed)
    val paragraphs = mutableListOf<String>()
    val current = StringBuilder()
    var count = 0
    for (sentence in sentences) {
        if (current.isNotEmpty()) current.append(' ')
        current.append(sentence)
        count++
        if (current.length >= PARAGRAPH_TARGET_CHARS || count >= PARAGRAPH_MAX_SENTENCES) {
            paragraphs += current.toString()
            current.clear()
            count = 0
        }
    }
    if (current.isNotEmpty()) paragraphs += current.toString()
    return paragraphs
}

private fun splitSentences(text: String): List<String> {
    val result = mutableListOf<String>()
    var start = 0
    var i = 0
    while (i < text.length) {
        val c = text[i]
        val spaced = i + 2 < text.length && text[i + 1] == ' ' && startsSentence(text[i + 2])
        // "yanılsama mı?Bu bölümde" gibi boşluksuz bitişik cümleler: ? ! ya da küçük harften sonra gelen nokta.
        val glued = i > 0 && i + 1 < text.length && text[i + 1].isUpperCase() &&
            (c == '?' || c == '!' || (c == '.' && text[i - 1].isLowerCase()))
        if (c in ".!?…" && (spaced || glued)) {
            val wordStart = text.lastIndexOf(' ', i - 1).let { if (it < start) start else it + 1 }
            val word = text.substring(wordStart, i)
            // "Dr.", "vb.", "No." gibi kısa kısaltmalarda bölme.
            if (!(c == '.' && word.length <= 2)) {
                result += text.substring(start, i + 1).trim()
                start = if (spaced) i + 2 else i + 1
            }
        }
        i++
    }
    if (start < text.length) result += text.substring(start).trim()
    return result.filter { it.isNotEmpty() }
}

private fun startsSentence(c: Char): Boolean =
    c.isUpperCase() || c.isDigit() || c == '"' || c == '“' || c == '(' || c == '‘'

/** "05:04" / "1:02:03" → ms; 00:00'a da izin verir (bölüm başlangıcı). */
private fun parseClockMs(text: String): Long? {
    val numbers = text.split(":").map { it.toLongOrNull() ?: return null }
    if (numbers.any { it < 0 }) return null
    val seconds = when (numbers.size) {
        2 -> numbers[0] * 60 + numbers[1]
        3 -> numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
        else -> return null
    }
    return seconds * 1000
}
