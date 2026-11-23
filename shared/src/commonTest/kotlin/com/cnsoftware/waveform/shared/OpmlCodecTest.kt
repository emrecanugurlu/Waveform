package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.rss.OpmlCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpmlCodecTest {

    @Test
    fun exportEscapesXmlEntitiesAndRoundTripsFeedUrls() {
        val subs = listOf(
            Podcast(
                feedUrl = "https://example.com/feed.xml?a=1&b=2",
                title = """Tech & "Code" <Show>""",
                author = "Author",
                description = "",
                imageUrl = null,
                episodes = emptyList()
            ),
            Podcast(
                feedUrl = "http://audio.example.org/rss",
                title = "Audio Daily",
                author = "Host",
                description = "",
                imageUrl = null,
                episodes = emptyList()
            )
        )

        val opml = OpmlCodec.export(subs)

        assertTrue("""<opml version="2.0">""" in opml)
        assertTrue("""title="Tech &amp; &quot;Code&quot; &lt;Show&gt;"""" in opml)
        assertTrue("""xmlUrl="https://example.com/feed.xml?a=1&amp;b=2"""" in opml)
        assertEquals(
            listOf(
                "https://example.com/feed.xml?a=1&b=2",
                "http://audio.example.org/rss"
            ),
            OpmlCodec.parseFeedUrls(opml)
        )
    }

    @Test
    fun parseFeedUrlsSupportsSingleQuotesDeduplicatesAndFiltersNonHttpUrls() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <body>
                <outline text="Category">
                  <outline type="rss" XMLURL='https://one.example.com/rss?x=1&amp;y=2' />
                  <outline type="rss" xmlUrl="https://one.example.com/rss?x=1&amp;y=2" />
                  <outline type="rss" xmlUrl="ftp://unsupported.example.com/feed" />
                  <outline type="rss" xmlUrl="not-a-url" />
                  <outline type="rss" xmlUrl="   https://two.example.com/feed.xml  " />
                </outline>
              </body>
            </opml>
        """.trimIndent()

        assertEquals(
            listOf(
                "https://one.example.com/rss?x=1&y=2",
                "https://two.example.com/feed.xml"
            ),
            OpmlCodec.parseFeedUrls(opml)
        )
        assertFalse(OpmlCodec.parseFeedUrls("   ").isNotEmpty())
    }
}
