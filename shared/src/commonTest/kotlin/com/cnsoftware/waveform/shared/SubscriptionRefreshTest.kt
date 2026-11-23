package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.data.mergeRefresh
import com.cnsoftware.waveform.shared.data.reconcileRefresh
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.NewEpisodeNotification
import com.cnsoftware.waveform.shared.model.Podcast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SubscriptionRefreshTest {

    private fun ep(id: String) = Episode(
        id = id, title = id, description = "", audioUrl = "https://x/$id.mp3",
        pubDate = "", durationText = "", imageUrl = null, podcastTitle = "P"
    )

    private fun podcast(feed: String, vararg ids: String) = Podcast(
        feedUrl = feed, title = "P", author = "", description = "",
        imageUrl = null, episodes = ids.map { ep(it) }
    )

    @Test
    fun reportsOnlyEpisodesThatWereNotKnownBefore() {
        val old = podcast("f", "e2", "e1")
        val fresh = podcast("f", "e3", "e2", "e1")

        val outcome = mergeRefresh(listOf(old to fresh), emptyList())

        assertEquals(listOf(fresh), outcome.subscriptions)
        assertEquals(listOf("e3"), outcome.newNotifications.map { it.episode.id })
        assertEquals(listOf("e3"), outcome.notifications.map { it.episode.id })
    }

    @Test
    fun keepsOldSubscriptionWhenFetchFails() {
        val old = podcast("f", "e1")
        val outcome = mergeRefresh(listOf(old to null), emptyList())

        assertSame(old, outcome.subscriptions.single())
        assertEquals(emptyList(), outcome.newNotifications)
    }

    @Test
    fun doesNotNotifyTwiceForTheSameEpisode() {
        val old = podcast("f", "e1")
        val fresh = podcast("f", "e2", "e1")
        val alreadyNotified = NewEpisodeNotification(ep("e2"), "f", isRead = true)

        val outcome = mergeRefresh(listOf(old to fresh), listOf(alreadyNotified))

        assertEquals(emptyList(), outcome.newNotifications)
        assertEquals(listOf(alreadyNotified), outcome.notifications)
    }

    @Test
    fun newNotificationsComeFirstAndSpanSeveralPodcasts() {
        val a = podcast("a", "a1")
        val b = podcast("b", "b1")
        val existing = NewEpisodeNotification(ep("old"), "a")

        val outcome = mergeRefresh(
            listOf(a to podcast("a", "a2", "a1"), b to podcast("b", "b2", "b1")),
            listOf(existing)
        )

        assertEquals(listOf("a2", "b2", "old"), outcome.notifications.map { it.episode.id })
        assertEquals(listOf("a2", "b2"), outcome.newNotifications.map { it.episode.id })
    }

    @Test
    fun reconcileDropsUnsubscribedPodcastRemovedMidRefresh() {
        val a = podcast("a", "a1")
        val b = podcast("b", "b1")
        val freshA = podcast("a", "a2", "a1")
        val freshB = podcast("b", "b2", "b1")

        // Yenileme sirasinda kullanici "b" aboneliginden cikti; sadece "a" kaldi.
        val outcome = reconcileRefresh(
            results = listOf(a to freshA, b to freshB),
            latestSubscriptions = listOf(a),
            currentNotifications = emptyList()
        )

        assertEquals(listOf(freshA), outcome.subscriptions)
        assertEquals(listOf("a2"), outcome.newNotifications.map { it.episode.id })
    }

    @Test
    fun reconcilePreservesNewlyAddedPodcastMidRefresh() {
        val a = podcast("a", "a1")
        val freshA = podcast("a", "a2", "a1")
        val newlyAddedC = podcast("c", "c1")

        // Yenileme sirasinda kullanici "c" podcast'ini yeni ekledi.
        val outcome = reconcileRefresh(
            results = listOf(a to freshA),
            latestSubscriptions = listOf(newlyAddedC, a),
            currentNotifications = emptyList()
        )

        assertEquals(listOf(newlyAddedC, freshA), outcome.subscriptions)
        assertEquals(listOf("a2"), outcome.newNotifications.map { it.episode.id })
    }
}
