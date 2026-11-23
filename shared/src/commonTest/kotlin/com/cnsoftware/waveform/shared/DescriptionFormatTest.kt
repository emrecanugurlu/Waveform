package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.rss.RssParser
import com.cnsoftware.waveform.shared.ui.parseEpisodeDescription
import com.cnsoftware.waveform.shared.ui.toParagraphs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DescriptionFormatTest {

    private val sample = "Bir metro anonsu nasıl olur? Bu bölümde Mind the Gap anonsunu anlatıyoruz. " +
        "Bu bölümde: (00:00) Intro (01:33) Embankment'taki ses (05:04) New York turnikeleri " +
        "(26:58) Bu sesleri kim seçiyor? Sunan: Barış Özcan Yapımcı: Podbee Media Tüm bölümler, " +
        "videolar ve daha fazlasına Podbee App ve podbee.com'dan ücretsiz olarak ulaşabilirsiniz."

    @Test
    fun extractsChaptersWithTimesAndLabels() {
        val parsed = parseEpisodeDescription(sample)
        assertEquals(4, parsed.chapters.size)
        assertEquals(0L, parsed.chapters[0].timeMs)
        assertEquals("Intro", parsed.chapters[0].label)
        assertEquals(93_000L, parsed.chapters[1].timeMs)
        assertEquals("Embankment'taki ses", parsed.chapters[1].label)
        assertEquals(26 * 60_000L + 58_000L, parsed.chapters[3].timeMs)
        assertEquals("Bu sesleri kim seçiyor?", parsed.chapters[3].label)
    }

    @Test
    fun trailingTextAfterLastChapterBecomesParagraphAndIntroLabelIsDropped() {
        val parsed = parseEpisodeDescription(sample)
        assertTrue(parsed.paragraphs.none { it.endsWith("Bu bölümde:") || it.endsWith("Bu bölümde") })
        assertTrue(parsed.paragraphs.any { it.startsWith("Sunan:") })
        assertTrue(parsed.paragraphs.first().startsWith("Bir metro anonsu"))
    }

    @Test
    fun plainTextWithoutTimestampsHasNoChapters() {
        val parsed = parseEpisodeDescription("Kısa bir açıklama. Saat 10:30 gibi başladık.")
        assertTrue(parsed.chapters.isEmpty())
        assertEquals(1, parsed.paragraphs.size)
    }

    @Test
    fun descendingTimesAreNotChapters() {
        assertTrue(parseEpisodeDescription("A 20:00 sonra 05:00 oldu").chapters.isEmpty())
    }

    @Test
    fun flatTextIsSplitIntoSentenceGroups() {
        val text = "Birinci cümle burada. İkinci cümle burada. Üçüncü cümle burada. Dördüncü cümle burada."
        val paragraphs = toParagraphs(text)
        assertEquals(2, paragraphs.size)
        assertEquals("Birinci cümle burada. İkinci cümle burada. Üçüncü cümle burada.", paragraphs[0])
    }

    @Test
    fun shortAbbreviationsDoNotSplitSentences() {
        assertEquals(1, toParagraphs("Konuğumuz Dr. Ahmet Yılmaz bugün anlatıyor.").size)
    }

    @Test
    fun existingLineBreaksAreKept() {
        assertEquals(listOf("Bir", "İki"), toParagraphs("Bir\n\nİki"))
    }

    @Test
    fun parserKeepsParagraphsAndBulletsInEpisodeDescriptions() {
        val html = "<p>Merhaba &amp; hoş geldin</p><p>İkinci <b>paragraf</b><br/>satır</p><ul><li>Bir</li><li>İki</li></ul>"
        assertEquals(
            "Merhaba & hoş geldin\nİkinci paragraf\nsatır\n• Bir\n• İki",
            RssParser.cleanDescription(html)
        )
        assertEquals("Düz", RssParser.cleanDescription("<![CDATA[Düz]]>"))
        assertEquals("", RssParser.cleanDescription(null))
    }
}

class DescriptionFormatCreditsTest {
    @Test
    fun gluedSentencesAreSplit() {
        val paragraphs = toParagraphs("Biz miyiz, yoksa bir yanılsama mı?Bu bölümde Freud'u anlatıyoruz.")
        assertEquals(1, paragraphs.size)
        assertTrue(paragraphs[0].contains("mı? Bu"))
    }

    @Test
    fun creditsAfterLastChapterBecomeSeparateLines() {
        val text = "Giriş metni. (00:00) Başlangıç (05:10) Doppelgänger ve Korku Sinemasında Aynalar " +
            "Sunan: Barış ÖzcanYapımcı: Podbee Media Tüm bölümler, videolar ve daha fazlası."
        val parsed = parseEpisodeDescription(text)
        assertEquals("Doppelgänger ve Korku Sinemasında Aynalar", parsed.chapters.last().label)
        assertTrue(parsed.paragraphs.any { it.startsWith("Sunan:") })
        assertTrue(parsed.paragraphs.any { it.startsWith("Yapımcı:") })
        assertTrue(parsed.paragraphs.any { it.startsWith("Tüm bölümler") })
    }
}
