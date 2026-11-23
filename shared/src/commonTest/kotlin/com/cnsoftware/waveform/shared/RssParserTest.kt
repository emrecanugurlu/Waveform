package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.rss.RssParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RssParserTest {

    @Test
    fun parsesValidPodcastFeedWithCdataAndItunesTags() {
        val sampleXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title><![CDATA[Teknoloji & Kod]]></title>
                <itunes:author>Ahmet Yilmaz</itunes:author>
                <description><![CDATA[<p>Haftalik <b>yazilim</b> sohbetleri.</p>]]></description>
                <itunes:image href="https://example.com/cover.jpg" />
                <item>
                  <title>Bolum 1: KMP Baslangic</title>
                  <guid>ep-1</guid>
                  <pubDate>Mon, 10 Nov 2026 10:00:00 GMT</pubDate>
                  <itunes:duration>42:15</itunes:duration>
                  <enclosure url="https://example.com/ep1.mp3" type="audio/mpeg" />
                </item>
                <item>
                  <title>Ses Dosyasi Olmayan Oge</title>
                  <description>Atlanmali</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val podcast = RssParser.parseFeed("https://example.com/feed.xml", sampleXml)

        assertEquals("Teknoloji & Kod", podcast.title)
        assertEquals("Ahmet Yilmaz", podcast.author)
        assertEquals("Haftalik yazilim sohbetleri.", podcast.description)
        assertEquals("https://example.com/cover.jpg", podcast.imageUrl)
        assertEquals(1, podcast.episodes.size)

        val episode = podcast.episodes.first()
        assertEquals("ep-1", episode.id)
        assertEquals("Bolum 1: KMP Baslangic", episode.title)
        assertEquals("https://example.com/ep1.mp3", episode.audioUrl)
        assertEquals("42:15", episode.durationText)
    }

    @Test
    fun decodesNumericAndNamedEntities() {
        val xml = """
            <rss><channel>
              <title>Ali&#8217;nin &#x27;Podcast&#x27;i &amp; Dostlari</title>
              <description>&lt;p&gt;Merhaba &lt;b&gt;dunya&lt;/b&gt;&lt;/p&gt; &mdash; &amp;amp; devam</description>
              <item>
                <title>Bolum &#128512;</title>
                <enclosure url="https://example.com/a.mp3" />
              </item>
            </channel></rss>
        """.trimIndent()

        val podcast = RssParser.parseFeed("https://example.com/f.xml", xml)

        assertEquals("Ali’nin 'Podcast'i & Dostlari", podcast.title)
        assertEquals("Merhaba dunya — &amp; devam", podcast.description)
        assertEquals("Bolum 😀", podcast.episodes.single().title)
    }

    @Test
    fun invalidEntitiesAreLeftUntouched() {
        assertEquals("&#99999999; &unknown; &#0;", RssParser.decodeEntities("&#99999999; &unknown; &#0;"))
    }

    @Test
    fun throwsWhenChannelTagIsMissing() {
        assertFailsWith<IllegalArgumentException> {
            RssParser.parseFeed("https://example.com/bad.xml", "<rss><invalid/></rss>")
        }
    }

    @Test
    fun decodesEntitiesInEnclosureAndImageUrls() {
        val xml = """
            <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
              <title>Test Podcast</title>
              <itunes:image href="https://example.com/cover.jpg?w=600&amp;fit=crop" />
              <item>
                <title>Bolum 1</title>
                <enclosure url="https://example.com/ep1.mp3?token=abc&amp;exp=123" />
              </item>
            </channel></rss>
        """.trimIndent()

        val podcast = RssParser.parseFeed("https://example.com/feed.xml", xml)

        assertEquals("https://example.com/cover.jpg?w=600&fit=crop", podcast.imageUrl)
        assertEquals(
            "https://example.com/ep1.mp3?token=abc&exp=123",
            podcast.episodes.single().audioUrl
        )
    }

    @Test
    fun usesStableAudioUrlWhenGuidIsMissing() {
        val initialXml = """
            <rss><channel>
              <title>Test Podcast</title>
              <item>
                <title>Eski Bolum</title>
                <enclosure url="https://example.com/ep1.mp3" />
              </item>
            </channel></rss>
        """.trimIndent()
        val updatedXml = """
            <rss><channel>
              <title>Test Podcast</title>
              <item>
                <title>Yeni Bolum</title>
                <enclosure url="https://example.com/ep2.mp3" />
              </item>
              <item>
                <title>Eski Bolum</title>
                <enclosure url="https://example.com/ep1.mp3" />
              </item>
              <item>
                <title>Eski Bolum Kopya</title>
                <enclosure url="https://example.com/ep1.mp3" />
              </item>
            </channel></rss>
        """.trimIndent()

        val firstParse = RssParser.parseFeed("https://example.com/feed.xml", initialXml)
        val secondParse = RssParser.parseFeed("https://example.com/feed.xml", updatedXml)

        assertEquals("https://example.com/ep1.mp3", firstParse.episodes.single().id)
        assertEquals(2, secondParse.episodes.size)
        assertEquals("https://example.com/ep2.mp3", secondParse.episodes[0].id)
        assertEquals("https://example.com/ep1.mp3", secondParse.episodes[1].id)
    }

    @Test
    fun prefersContentEncodedForEpisodeDescription() {
        val xml = """
            <rss xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel>
              <title>Notlar</title>
              <item>
                <title>Bolum 5</title>
                <description>Kisa ozet</description>
                <content:encoded><![CDATA[<p>Detayli <b>bolum</b> notlari.</p><p>(01:15) Giris</p>]]></content:encoded>
                <enclosure url="https://example.com/ep5.mp3" />
              </item>
            </channel></rss>
        """.trimIndent()

        val podcast = RssParser.parseFeed("https://example.com/feed.xml", xml)

        assertEquals(
            "Detayli bolum notlari.\n(01:15) Giris",
            podcast.episodes.single().description
        )
    }
}
