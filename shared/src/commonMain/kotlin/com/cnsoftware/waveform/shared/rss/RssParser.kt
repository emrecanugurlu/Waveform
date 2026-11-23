package com.cnsoftware.waveform.shared.rss

import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.Podcast

/**
 * Lightweight, dependency-free RSS 2.0 / iTunes podcast XML parser for KMP commonMain.
 */
object RssParser {

    fun parseFeed(feedUrl: String, xml: String): Podcast {
        val channelContent = extractTagContent(xml, "channel")
            ?: throw IllegalArgumentException("Geçerli bir RSS <channel> etiketi bulunamadı.")

        val itemsStart = channelContent.indexOf("<item", ignoreCase = true)
        val channelHeader = if (itemsStart >= 0) {
            channelContent.substring(0, itemsStart)
        } else {
            channelContent
        }

        val podcastTitle = cleanText(extractTagContent(channelHeader, "title"))
            .ifBlank { "İsimsiz Podcast" }
        val author = cleanText(
            extractTagContent(channelHeader, "itunes:author")
                ?: extractTagContent(channelHeader, "author")
        )
        val description = cleanText(
            extractTagContent(channelHeader, "itunes:summary")
                ?: extractTagContent(channelHeader, "description")
        )
        val podcastImage = extractItunesImageHref(channelHeader)
            ?: extractNestedImageUrl(channelHeader)

        val episodes = extractAllBlocks(channelContent, "item").mapIndexedNotNull { index, itemXml ->
            parseEpisode(
                itemXml = itemXml,
                index = index,
                podcastTitle = podcastTitle,
                fallbackImage = podcastImage
            )
        }.distinctBy { it.id }

        return Podcast(
            feedUrl = feedUrl,
            title = podcastTitle,
            author = author,
            description = description,
            imageUrl = podcastImage,
            episodes = episodes
        )
    }

    private fun parseEpisode(
        itemXml: String,
        index: Int,
        podcastTitle: String,
        fallbackImage: String?
    ): Episode? {
        val audioUrl = extractAttribute(itemXml, "enclosure", "url")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val title = cleanText(extractTagContent(itemXml, "title"))
            .ifBlank { "Bölüm ${index + 1}" }
        val description = listOfNotNull(
            extractTagContent(itemXml, "content:encoded"),
            extractTagContent(itemXml, "itunes:summary"),
            extractTagContent(itemXml, "description")
        ).map { cleanDescription(it) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val pubDate = cleanText(extractTagContent(itemXml, "pubDate"))
        val duration = cleanText(extractTagContent(itemXml, "itunes:duration"))
        val guid = cleanText(extractTagContent(itemXml, "guid")).ifBlank { audioUrl }
        val episodeImage = extractItunesImageHref(itemXml) ?: fallbackImage

        return Episode(
            id = guid,
            title = title,
            description = description,
            audioUrl = audioUrl,
            pubDate = pubDate,
            durationText = duration,
            imageUrl = episodeImage,
            podcastTitle = podcastTitle
        )
    }

    private fun buildTagRegex(tagName: String): Regex = Regex(
        pattern = "<$tagName(?:\\s[^>]*)?>(.*?)</$tagName>",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private fun buildAttrRegexPair(tagName: String, attrName: String): Pair<Regex, Regex> =
        Regex(
            pattern = "<$tagName\\s+([^>]*)/?>",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ) to Regex(
            pattern = """(?:^|\s)$attrName\s*=\s*(?:"([^"]+)"|'([^']+)')""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

    private val PRECOMPILED_TAG_REGEX = listOf(
        "channel", "title", "itunes:author", "author", "itunes:summary",
        "content:encoded", "description", "item", "pubDate",
        "itunes:duration", "guid", "image", "url"
    ).associateWith { buildTagRegex(it) }

    private val PRECOMPILED_ATTR_REGEX = mapOf(
        ("enclosure" to "url") to buildAttrRegexPair("enclosure", "url"),
        ("itunes:image" to "href") to buildAttrRegexPair("itunes:image", "href")
    )

    private fun extractTagContent(xml: String, tagName: String): String? {
        val regex = PRECOMPILED_TAG_REGEX[tagName] ?: buildTagRegex(tagName)
        return regex.find(xml)?.groupValues?.getOrNull(1)
    }

    private fun extractAllBlocks(xml: String, tagName: String): List<String> {
        val regex = PRECOMPILED_TAG_REGEX[tagName] ?: buildTagRegex(tagName)
        return regex.findAll(xml).map { it.groupValues[1] }.toList()
    }

    private fun extractAttribute(xml: String, tagName: String, attrName: String): String? {
        val (tagRegex, attrRegex) = PRECOMPILED_ATTR_REGEX[tagName to attrName]
            ?: buildAttrRegexPair(tagName, attrName)
        val attrs = tagRegex.find(xml)?.groupValues?.getOrNull(1) ?: return null
        val match = attrRegex.find(attrs) ?: return null
        val raw = match.groupValues[1].ifEmpty { match.groupValues[2] }
        return decodeEntities(raw)
    }

    private fun extractItunesImageHref(xml: String): String? =
        extractAttribute(xml, "itunes:image", "href")?.trim()?.takeIf { it.isNotEmpty() }

    private fun extractNestedImageUrl(xml: String): String? {
        val imageBlock = extractTagContent(xml, "image") ?: return null
        return cleanText(extractTagContent(imageBlock, "url")).takeIf { it.isNotEmpty() }
    }

    private fun cleanText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var text = raw.trim()
        if (text.startsWith("<![CDATA[") && text.endsWith("]]>")) {
            text = text.removePrefix("<![CDATA[").removeSuffix("]]>")
        }
        // Etiketleri once temizle, sonra varliklari coz. Bircok feed aciklamayi HTML-kacisli
        // (&lt;p&gt;...) gonderdigi icin cozumden sonra bir tur daha etiket temizligi yapilir.
        val decoded = decodeEntities(text.replace(TAG_REGEX, " "))
        val stripped = if (decoded.contains('<')) decoded.replace(TAG_REGEX, " ") else decoded
        return stripped
            .replace(' ', ' ')
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    /**
     * Bölüm açıklaması için: [cleanText] gibi etiketleri temizler ama paragraf yapısını korur
     * (<br>, </p>, </li> vb. satır sonu olur, <li> madde işareti alır). Satırlar "\n" ile ayrılır.
     */
    internal fun cleanDescription(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var text = raw.trim()
        if (text.startsWith("<![CDATA[") && text.endsWith("]]>")) {
            text = text.removePrefix("<![CDATA[").removeSuffix("]]>")
        }
        fun flatten(t: String) = t
            .replace(LIST_ITEM_REGEX, "\n• ")
            .replace(BLOCK_BREAK_REGEX, "\n")
            .replace(TAG_REGEX, " ")
        val decoded = decodeEntities(flatten(text))
        val stripped = if (decoded.contains('<')) flatten(decoded) else decoded
        return stripped
            .replace(' ', ' ')
            .split('\n')
            .map { it.replace(WHITESPACE_REGEX, " ").trim() }
            .filter { it.isNotEmpty() && it != "•" }
            .joinToString("\n")
    }

    private val LIST_ITEM_REGEX = Regex("<li(?:\\s[^>]*)?>", RegexOption.IGNORE_CASE)
    private val BLOCK_BREAK_REGEX = Regex(
        "<br\\s*/?>|</p\\s*>|</div\\s*>|</li\\s*>|</h[1-6]\\s*>|</ul\\s*>|</ol\\s*>",
        RegexOption.IGNORE_CASE
    )
    private val TAG_REGEX = Regex("<[^>]+>")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val ENTITY_REGEX = Regex("&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "nbsp" to " ", "ndash" to "–", "mdash" to "—",
        "hellip" to "…", "lsquo" to "‘", "rsquo" to "’",
        "ldquo" to "“", "rdquo" to "”"
    )

    /** Tek geciste cozer; boylece "&amp;lt;" gibi metinler cift cozulmez. */
    internal fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        return ENTITY_REGEX.replace(text) { match ->
            val body = match.groupValues[1]
            when {
                body.startsWith("#x", ignoreCase = true) ->
                    codePointToString(body.substring(2).toIntOrNull(16)) ?: match.value
                body.startsWith("#") ->
                    codePointToString(body.substring(1).toIntOrNull()) ?: match.value
                else -> NAMED_ENTITIES[body] ?: match.value
            }
        }
    }

    private fun codePointToString(codePoint: Int?): String? {
        if (codePoint == null || codePoint !in 1..0x10FFFF) return null
        if (codePoint in 0xD800..0xDFFF) return null
        return if (codePoint < 0x10000) {
            codePoint.toChar().toString()
        } else {
            val offset = codePoint - 0x10000
            charArrayOf(
                (0xD800 + (offset shr 10)).toChar(),
                (0xDC00 + (offset and 0x3FF)).toChar()
            ).concatToString()
        }
    }
}
