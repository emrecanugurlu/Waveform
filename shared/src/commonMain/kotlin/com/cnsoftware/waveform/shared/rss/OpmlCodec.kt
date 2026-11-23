package com.cnsoftware.waveform.shared.rss

import com.cnsoftware.waveform.shared.model.Podcast

/**
 * Abonelik listesini standart OPML 2.0 XML formatında dışa aktarır ve içe aktarım için besleme
 * URL'lerini ayıklar.
 */
object OpmlCodec {

    private val OUTLINE_TAG_REGEX = Regex(
        pattern = "<outline\\s+([^>]*?)/?>",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val XML_URL_ATTR_REGEX = Regex(
        pattern = """(?:^|\s)xmlUrl\s*=\s*(?:"([^"]+)"|'([^']+)')""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun export(subscriptions: List<Podcast>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<opml version="2.0">""")
        appendLine("  <head>")
        appendLine("    <title>Waveform Abonelikleri</title>")
        appendLine("  </head>")
        appendLine("  <body>")
        subscriptions.forEach { podcast ->
            val title = escapeXml(podcast.title)
            val feedUrl = escapeXml(podcast.feedUrl)
            appendLine("""    <outline type="rss" text="$title" title="$title" xmlUrl="$feedUrl" />""")
        }
        appendLine("  </body>")
        append("</opml>")
    }

    fun parseFeedUrls(opmlXml: String): List<String> {
        if (opmlXml.isBlank()) return emptyList()
        return OUTLINE_TAG_REGEX.findAll(opmlXml)
            .mapNotNull { tagMatch ->
                val attrs = tagMatch.groupValues[1]
                val attrMatch = XML_URL_ATTR_REGEX.find(attrs) ?: return@mapNotNull null
                val rawUrl = attrMatch.groupValues[1].ifEmpty { attrMatch.groupValues[2] }
                val decoded = RssParser.decodeEntities(rawUrl).trim()
                decoded.takeIf {
                    it.startsWith("http://", ignoreCase = true) ||
                        it.startsWith("https://", ignoreCase = true)
                }
            }
            .distinct()
            .toList()
    }

    private fun escapeXml(text: String): String = buildString(text.length) {
        for (ch in text) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}
