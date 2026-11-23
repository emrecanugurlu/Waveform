package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.data.PodcastStore
import com.cnsoftware.waveform.shared.data.TextStorage
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.EpisodeBookmark
import com.cnsoftware.waveform.shared.model.ListeningHistoryEntry
import com.cnsoftware.waveform.shared.model.ListeningStats
import com.cnsoftware.waveform.shared.model.Podcast
import kotlin.test.Test
import com.cnsoftware.waveform.shared.ui.PlayQueue
import com.cnsoftware.waveform.shared.ui.ThemeMode
import com.cnsoftware.waveform.shared.ui.ThemePalette
import com.cnsoftware.waveform.shared.ui.isDark
import com.cnsoftware.waveform.shared.ui.findNextEpisode
import com.cnsoftware.waveform.shared.ui.findOrphanedDownloadIds
import com.cnsoftware.waveform.shared.ui.looksLikeFeedUrl
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class InMemoryTextStorage : TextStorage {
    private val files = mutableMapOf<String, String>()

    override fun readText(fileName: String): String? = files[fileName]

    override fun writeText(fileName: String, content: String) {
        files[fileName] = content
    }
}

private fun episode(id: String, podcast: String = "P") = Episode(
    id = id, title = id, description = "", audioUrl = "https://x/$id.mp3",
    pubDate = "", durationText = "", imageUrl = null, podcastTitle = podcast
)

private fun podcast(title: String, vararg ids: String) = Podcast(
    feedUrl = "https://x/$title", title = title, author = "", description = "",
    imageUrl = null, episodes = ids.map { episode(it, title) }
)

class NextEpisodeTest {

    @Test
    fun picksNewerEpisodeAboveCurrentInFeedOrder() {
        // Feed yeniden eskiye: e3 en yeni.
        val subs = listOf(podcast("P", "e3", "e2", "e1"))
        assertEquals("e3", findNextEpisode(subs, episode("e2", "P"))?.id)
        assertEquals("e2", findNextEpisode(subs, episode("e1", "P"))?.id)
    }

    @Test
    fun stopsAtNewestEpisode() {
        val subs = listOf(podcast("P", "e3", "e2", "e1"))
        assertNull(findNextEpisode(subs, episode("e3", "P")))
    }

    @Test
    fun skipsPlayedEpisodesWhenLookingForNext() {
        val subs = listOf(podcast("P", "e4", "e3", "e2", "e1"))
        assertEquals("e4", findNextEpisode(subs, episode("e1", "P"), setOf("e2", "e3"))?.id)
        assertEquals("e3", findNextEpisode(subs, episode("e1", "P"), setOf("e2"))?.id)
        // Daha yeni bolumlerin hepsi dinlendiyse durur.
        assertNull(findNextEpisode(subs, episode("e1", "P"), setOf("e2", "e3", "e4")))
    }

    @Test
    fun returnsNullWhenEpisodeIsNotInAnySubscription() {
        assertNull(findNextEpisode(listOf(podcast("P", "e2", "e1")), episode("zzz")))
        assertNull(findNextEpisode(emptyList(), episode("e1")))
    }

    @Test
    fun prefersPodcastMatchingTitleWhenIdsCollide() {
        val subs = listOf(
            podcast("A", "a2", "shared"),
            podcast("B", "b2", "shared")
        )
        assertEquals("b2", findNextEpisode(subs, episode("shared", "B"))?.id)
    }
}

class FeedUrlDetectionTest {
    @Test
    fun recognizesHttpAndHttpsLinks() {
        assertEquals(true, looksLikeFeedUrl("https://example.com/feed.xml"))
        assertEquals(true, looksLikeFeedUrl("  HTTP://example.com/rss  "))
    }

    @Test
    fun treatsOrdinaryTextAsSearch() {
        assertEquals(false, looksLikeFeedUrl("Kotlin podcast"))
        assertEquals(false, looksLikeFeedUrl("example.com/feed"))
        assertEquals(false, looksLikeFeedUrl("https://"))
        assertEquals(false, looksLikeFeedUrl("https://example.com ve baska kelime"))
        assertEquals(false, looksLikeFeedUrl(""))
    }
}

class PlayQueueTest {
    private fun ids(queue: List<Episode>) = queue.map { it.id }

    @Test
    fun addLastAppendsAndIgnoresDuplicates() {
        var queue = PlayQueue.addLast(emptyList(), episode("a"))
        queue = PlayQueue.addLast(queue, episode("b"))
        queue = PlayQueue.addLast(queue, episode("a"))
        assertEquals(listOf("a", "b"), ids(queue))
    }

    @Test
    fun removeDropsOnlyMatchingEpisode() {
        val queue = listOf(episode("a"), episode("b"), episode("c"))
        assertEquals(listOf("a", "c"), ids(PlayQueue.remove(queue, "b")))
        assertEquals(listOf("a", "b", "c"), ids(PlayQueue.remove(queue, "zzz")))
    }

    @Test
    fun moveUpSwapsWithPreviousAndKeepsEdgesStable() {
        val queue = listOf(episode("a"), episode("b"), episode("c"))
        assertEquals(listOf("a", "c", "b"), ids(PlayQueue.moveUp(queue, "c")))
        assertEquals(listOf("a", "b", "c"), ids(PlayQueue.moveUp(queue, "a")))
        assertEquals(listOf("a", "b", "c"), ids(PlayQueue.moveUp(queue, "zzz")))
    }

    @Test
    fun moveDownSwapsWithNextAndKeepsEdgesStable() {
        val queue = listOf(episode("a"), episode("b"), episode("c"))
        assertEquals(listOf("b", "a", "c"), ids(PlayQueue.moveDown(queue, "a")))
        assertEquals(listOf("a", "b", "c"), ids(PlayQueue.moveDown(queue, "c")))
        assertEquals(listOf("a", "b", "c"), ids(PlayQueue.moveDown(queue, "zzz")))
    }

    @Test
    fun queuePersistsInOrder() {
        val storage = InMemoryTextStorage()
        val queue = listOf(episode("x"), episode("y"))
        PodcastStore(storage).saveQueue(queue)
        assertEquals(queue, PodcastStore(storage).loadQueue())

        storage.writeText("queue.json", "bozuk")
        assertEquals(emptyList(), PodcastStore(storage).loadQueue())
    }
}

class PodcastStoreTest {

    @Test
    fun playedEpisodeIdsPersistAndSurviveCorruptData() {
        val storage = InMemoryTextStorage()
        assertEquals(emptySet(), PodcastStore(storage).loadPlayedEpisodeIds())

        PodcastStore(storage).savePlayedEpisodeIds(setOf("a", "b"))
        assertEquals(setOf("a", "b"), PodcastStore(storage).loadPlayedEpisodeIds())

        storage.writeText("played_episodes.json", "bozuk")
        assertEquals(emptySet(), PodcastStore(storage).loadPlayedEpisodeIds())
    }

    @Test
    fun themeModeDefaultsToSystemAndPersistsAndSurvivesBadData() {
        val storage = InMemoryTextStorage()
        assertEquals(ThemeMode.SYSTEM, PodcastStore(storage).loadThemeMode())

        PodcastStore(storage).saveThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, PodcastStore(storage).loadThemeMode())

        storage.writeText("theme_mode.txt", "pembe")
        assertEquals(ThemeMode.SYSTEM, PodcastStore(storage).loadThemeMode())
    }

    @Test
    fun themeModeResolvesDarkOrLight() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
        assertEquals(false, ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemInDarkTheme = false))
    }

    @Test
    fun autoPlayNextDefaultsToTrueAndPersists() {
        val storage = InMemoryTextStorage()
        assertEquals(true, PodcastStore(storage).loadAutoPlayNext())

        PodcastStore(storage).saveAutoPlayNext(false)
        assertEquals(false, PodcastStore(storage).loadAutoPlayNext())

        storage.writeText("auto_play_next.txt", "bozuk")
        assertEquals(true, PodcastStore(storage).loadAutoPlayNext())
    }

    @Test
    fun savesAndLoadsSubscriptionsAndPositions() {
        val storage = InMemoryTextStorage()
        val store = PodcastStore(storage)

        val podcast = Podcast(
            feedUrl = "https://example.com/feed.xml",
            title = "KMP Podcast",
            author = "Emre",
            description = "Aciklama",
            imageUrl = "https://example.com/cover.jpg",
            episodes = listOf(
                Episode(
                    id = "ep-1",
                    title = "Bolum 1",
                    description = "Ilk bolum",
                    audioUrl = "https://example.com/ep1.mp3",
                    pubDate = "Mon, 10 Nov 2026",
                    durationText = "30:00",
                    imageUrl = null,
                    podcastTitle = "KMP Podcast"
                )
            )
        )

        store.saveSubscriptions(listOf(podcast))
        store.savePositions(mapOf("ep-1" to 125_000L))

        val reloadedStore = PodcastStore(storage)
        assertEquals(listOf(podcast), reloadedStore.loadSubscriptions())
        assertEquals(mapOf("ep-1" to 125_000L), reloadedStore.loadPositions())
    }

    @Test
    fun savesLoadsAndFallsBackForRedesignPreferencesBookmarksAndStats() {
        val storage = InMemoryTextStorage()
        val store = PodcastStore(storage)

        assertEquals(ThemePalette.STUDIO, store.loadThemePalette())
        assertEquals(1.0f, store.loadPlaybackSpeed())
        assertEquals(10, store.loadSkipBackSeconds())
        assertEquals(30, store.loadSkipForwardSeconds())
        assertEquals(emptyList(), store.loadBookmarks())
        assertEquals(ListeningStats(), store.loadListeningStats())
        assertEquals(emptyList(), store.loadRecentSearches())

        val ep = episode("ep-b1", "Studio Show")
        val bookmark = EpisodeBookmark(
            id = "bm-1",
            episode = ep,
            positionMs = 45_000L,
            note = "Giris",
            createdAtMs = 1_700_000_000_000L
        )
        val stats = ListeningStats(
            totalListeningSeconds = 420L,
            completedEpisodesCount = 3,
            activeDayKeys = setOf("2026-11-23"),
            podcastListeningSeconds = mapOf("Studio Show" to 420L)
        )

        store.saveThemePalette(ThemePalette.FOREST)
        store.savePlaybackSpeed(10.0f)
        store.saveSkipBackSeconds(0)
        store.saveSkipForwardSeconds(500)
        store.saveBookmarks(listOf(bookmark))
        store.saveListeningStats(stats)
        store.saveRecentSearches(listOf("kotlin", "bilim"))

        val reloaded = PodcastStore(storage)
        assertEquals(ThemePalette.FOREST, reloaded.loadThemePalette())
        assertEquals(3.0f, reloaded.loadPlaybackSpeed())
        assertEquals(1, reloaded.loadSkipBackSeconds())
        assertEquals(120, reloaded.loadSkipForwardSeconds())
        assertEquals(listOf(bookmark), reloaded.loadBookmarks())
        assertEquals(stats, reloaded.loadListeningStats())
        assertEquals(listOf("kotlin", "bilim"), reloaded.loadRecentSearches())

        storage.writeText("theme_palette.txt", "UNKNOWN")
        storage.writeText("playback_speed.txt", "NaN")
        storage.writeText("skip_back_seconds.txt", "999")
        storage.writeText("skip_forward_seconds.txt", "abc")
        storage.writeText("bookmarks.json", "{corrupt")
        storage.writeText("listening_stats.json", "{corrupt")
        storage.writeText("recent_searches.json", "{corrupt")

        assertEquals(ThemePalette.STUDIO, reloaded.loadThemePalette())
        assertEquals(1.0f, reloaded.loadPlaybackSpeed())
        assertEquals(10, reloaded.loadSkipBackSeconds())
        assertEquals(30, reloaded.loadSkipForwardSeconds())
        assertEquals(emptyList(), reloaded.loadBookmarks())
        assertEquals(ListeningStats(), reloaded.loadListeningStats())
        assertEquals(emptyList(), reloaded.loadRecentSearches())
    }

    @Test
    fun savesLoadsAndFallsBackForListeningHistoryAndDailyStats() {
        val storage = InMemoryTextStorage()
        val store = PodcastStore(storage)
        assertEquals(emptyList(), store.loadListeningHistory())

        val ep = episode("ep-hist", "Tarih Pod")
        val entry = ListeningHistoryEntry(
            episode = ep,
            lastPlayedAtMs = 1_794_830_400_000L,
            lastPositionMs = 120_000L,
            durationMs = 600_000L,
            totalListenedSeconds = 95L,
            playCount = 3,
            completed = false
        )
        val stats = ListeningStats(
            totalListeningSeconds = 95L,
            completedEpisodesCount = 1,
            activeDayKeys = setOf("2026-11-16"),
            podcastListeningSeconds = mapOf("Tarih Pod" to 95L),
            dailyListeningSeconds = mapOf("2026-11-16" to 95L)
        )

        store.saveListeningHistory(listOf(entry))
        store.saveListeningStats(stats)

        val reloaded = PodcastStore(storage)
        assertEquals(listOf(entry), reloaded.loadListeningHistory())
        assertEquals(stats, reloaded.loadListeningStats())

        storage.writeText("listening_history.json", "{corrupt")
        assertEquals(emptyList(), reloaded.loadListeningHistory())
    }
}

class OrphanedDownloadsTest {

    @Test
    fun removesOnlyTrulyOrphanedDownloadsWhenUnsubscribing() {
        val removed = podcast("Removed", "ep-orphan", "ep-fav", "ep-queue", "ep-recent", "ep-playing", "ep-shared")
        val remaining = listOf(podcast("Other", "ep-shared"))

        val orphaned = findOrphanedDownloadIds(
            removedPodcast = removed,
            remainingSubscriptions = remaining,
            favorites = listOf(episode("ep-fav", "Removed")),
            queue = listOf(episode("ep-queue", "Removed")),
            recentEpisodes = listOf(episode("ep-recent", "Removed")),
            downloadedIds = setOf("ep-orphan", "ep-fav", "ep-queue", "ep-recent", "ep-playing", "ep-shared", "ep-unrelated"),
            playingLocalId = "ep-playing"
        )

        assertEquals(setOf("ep-orphan"), orphaned)
    }
}
