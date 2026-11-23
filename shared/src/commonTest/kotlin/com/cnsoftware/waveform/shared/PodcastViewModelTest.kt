package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.data.AudioSink
import com.cnsoftware.waveform.shared.data.EpisodeFileStore
import com.cnsoftware.waveform.shared.data.PodcastRepository
import com.cnsoftware.waveform.shared.data.PodcastStore
import com.cnsoftware.waveform.shared.data.RefreshScheduler
import com.cnsoftware.waveform.shared.data.TextStorage
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.EpisodeBookmark
import com.cnsoftware.waveform.shared.model.ListeningStats
import com.cnsoftware.waveform.shared.model.NewEpisodeNotification
import com.cnsoftware.waveform.shared.model.PlayerState
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.player.AudioPlayer
import com.cnsoftware.waveform.shared.ui.AppTab
import com.cnsoftware.waveform.shared.ui.LibrarySection
import com.cnsoftware.waveform.shared.ui.PodcastViewModel
import com.cnsoftware.waveform.shared.ui.ThemeMode
import com.cnsoftware.waveform.shared.ui.ThemePalette
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeTextStorage : TextStorage {
    private val files = mutableMapOf<String, String>()

    override fun readText(fileName: String): String? = files[fileName]

    override fun writeText(fileName: String, content: String) {
        files[fileName] = content
    }
}

private class FakeEpisodeFileStore(
    initialFiles: Map<String, String> = emptyMap()
) : EpisodeFileStore {
    val files = initialFiles.toMutableMap()
    val deletedIds = mutableListOf<String>()

    override fun getLocalFileUri(episodeId: String): String? = files[episodeId]

    override fun openAudioSink(episodeId: String, audioUrl: String): AudioSink = object : AudioSink {
        override fun write(buffer: ByteArray, length: Int) = Unit

        override fun commit(): String {
            val uri = "file:///episodes/$episodeId.mp3"
            files[episodeId] = uri
            return uri
        }

        override fun abort() = Unit
    }

    override fun deleteAudio(episodeId: String) {
        deletedIds += episodeId
        files.remove(episodeId)
    }

    override fun listDownloadedIds(): Set<String> = files.keys.toSet()
}

private class FakeAudioPlayer : AudioPlayer {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    val played = mutableListOf<Pair<Episode, Long>>()
    var released = false

    override fun play(episode: Episode, startPositionMs: Long) {
        played += episode to startPositionMs
        _state.update {
            it.copy(
                currentEpisode = episode,
                isPlaying = true,
                isBuffering = false,
                isEnded = false,
                positionMs = startPositionMs,
                durationMs = 600_000L,
                errorMessage = null
            )
        }
    }

    override fun togglePlayPause() {
        _state.update { it.copy(isPlaying = !it.isPlaying) }
    }

    override fun seekTo(positionMs: Long) {
        _state.update { it.copy(positionMs = positionMs, isEnded = false) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
    }

    override fun release() {
        released = true
    }

    fun emitPosition(positionMs: Long) {
        _state.update {
            it.copy(
                isPlaying = true,
                isBuffering = false,
                isEnded = false,
                positionMs = positionMs
            )
        }
    }

    fun emitEnded() {
        _state.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                isEnded = true,
                positionMs = it.durationMs
            )
        }
    }
}

private class FakeRefreshScheduler(
    override val isSupported: Boolean = true
) : RefreshScheduler {
    val enabledCalls = mutableListOf<Boolean>()

    override fun setEnabled(enabled: Boolean) {
        enabledCalls += enabled
    }
}

private fun vmEpisode(id: String, podcast: String = "Podcast") = Episode(
    id = id,
    title = "Title $id",
    description = "",
    audioUrl = "https://example.com/$id.mp3",
    pubDate = "",
    durationText = "10:00",
    imageUrl = null,
    podcastTitle = podcast
)

private fun vmPodcast(feedUrl: String, title: String, vararg episodeIds: String) = Podcast(
    feedUrl = feedUrl,
    title = title,
    author = "Author",
    description = "",
    imageUrl = null,
    episodes = episodeIds.map { vmEpisode(it, title) }
)

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastViewModelTest {

    private lateinit var testScheduler: TestCoroutineScheduler
    private lateinit var mainDispatcher: kotlinx.coroutines.test.TestDispatcher

    @BeforeTest
    fun setUp() {
        testScheduler = TestCoroutineScheduler()
        mainDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        storage: TextStorage = FakeTextStorage(),
        fileStore: EpisodeFileStore = FakeEpisodeFileStore(),
        player: AudioPlayer = FakeAudioPlayer(),
        scheduler: RefreshScheduler = FakeRefreshScheduler(),
        httpClient: HttpClient = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
    ): PodcastViewModel = PodcastViewModel(
        repository = PodcastRepository(httpClient),
        store = PodcastStore(storage),
        episodeFileStore = fileStore,
        audioPlayer = player,
        backgroundRefresh = scheduler,
        ioDispatcher = mainDispatcher
    )

    @Test
    fun loadsPersistedStateAndConfiguresBackgroundRefresh() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val ep1 = vmEpisode("ep1")
        val ep2 = vmEpisode("ep2")
        val sub = vmPodcast("https://x/p", "Podcast", "ep1", "ep2")
        store.saveSubscriptions(listOf(sub))
        store.saveFavorites(listOf(ep1))
        store.saveQueue(listOf(ep2))
        store.savePlayedEpisodeIds(setOf("ep1"))
        store.saveThemeMode(ThemeMode.DARK)
        store.saveBackgroundRefresh(false)
        store.saveAutoPlayNext(false)

        val fileStore = FakeEpisodeFileStore(mapOf("ep1" to "file:///ep1.mp3"))
        val scheduler = FakeRefreshScheduler()
        val vm = createViewModel(storage = storage, fileStore = fileStore, scheduler = scheduler)

        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(sub), state.subscriptions)
        assertEquals(listOf(ep1), state.favorites)
        assertEquals(listOf(ep2), state.queue)
        assertEquals(setOf("ep1"), state.playedEpisodeIds)
        assertEquals(setOf("ep1"), state.downloadedEpisodeIds)
        assertFalse(state.autoPlayNext)
        assertFalse(state.backgroundRefreshEnabled)
        assertEquals(listOf(false), scheduler.enabledCalls)
    }

    @Test
    fun playEpisodeUsesLocalFileMarksReadUpdatesRecentAndRemovesFromQueue() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val ep1 = vmEpisode("ep1")
        val ep2 = vmEpisode("ep2")
        store.savePositions(mapOf("ep1" to 45_000L))
        store.saveQueue(listOf(ep1, ep2))
        store.saveNotifications(listOf(NewEpisodeNotification(ep1, "https://x/p", isRead = false)))

        val fileStore = FakeEpisodeFileStore(mapOf("ep1" to "file:///episodes/ep1.mp3"))
        val player = FakeAudioPlayer()
        val vm = createViewModel(storage = storage, fileStore = fileStore, player = player)
        advanceUntilIdle()

        vm.playEpisode(ep1)
        advanceUntilIdle()

        assertEquals(1, player.played.size)
        assertEquals("file:///episodes/ep1.mp3", player.played.single().first.audioUrl)
        assertEquals(45_000L, player.played.single().second)
        assertEquals(listOf(ep2), vm.uiState.value.queue)
        assertTrue(vm.uiState.value.notifications.single().isRead)
        assertEquals(listOf(ep1), vm.uiState.value.recentEpisodes)

        val reloaded = PodcastStore(storage)
        assertEquals(listOf(ep2), reloaded.loadQueue())
        assertTrue(reloaded.loadNotifications().single().isRead)
        assertEquals(listOf(ep1), reloaded.loadRecentEpisodes())
    }

    @Test
    fun episodeEndedPlaysQueuedEpisodeEvenWhenAutoPlayNextIsDisabled() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val ep1 = vmEpisode("ep1")
        val epQueued = vmEpisode("epQueued")
        store.saveAutoPlayNext(false)
        store.saveQueue(listOf(epQueued))
        store.savePositions(mapOf("ep1" to 120_000L))

        val player = FakeAudioPlayer()
        val vm = createViewModel(storage = storage, player = player)
        advanceUntilIdle()

        vm.playEpisode(ep1)
        advanceUntilIdle()

        player.emitEnded()
        advanceUntilIdle()

        assertTrue("ep1" in vm.uiState.value.playedEpisodeIds)
        assertFalse("ep1" in vm.uiState.value.episodePositions)
        assertEquals("epQueued", player.played.last().first.id)
        assertTrue(vm.uiState.value.queue.isEmpty())
    }

    @Test
    fun episodeEndedAutoPlaysNextUnplayedEpisodeAndRespectsSleepAtEndOfEpisode() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val sub = vmPodcast("https://x/p", "Podcast", "ep3", "ep2", "ep1")
        store.saveSubscriptions(listOf(sub))
        store.savePlayedEpisodeIds(setOf("ep2"))
        store.saveAutoPlayNext(true)

        val player = FakeAudioPlayer()
        val vm = createViewModel(storage = storage, player = player)
        advanceUntilIdle()

        vm.playEpisode(sub.episodes[2]) // ep1
        advanceUntilIdle()

        player.emitEnded()
        advanceUntilIdle()

        // ep2 dinlenmis oldugu icin atlaniyor, ep3 basliyor.
        assertEquals("ep3", player.played.last().first.id)

        vm.sleepAtEndOfEpisode()
        assertTrue(vm.uiState.value.sleepAtEndOfEpisode)

        val countBeforeEnd = player.played.size
        player.emitEnded()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.sleepAtEndOfEpisode)
        assertEquals(countBeforeEnd, player.played.size)
    }

    @Test
    fun toggleDownloadEpisodeProtectsPlayingLocalFileAndDeletesOtherLocalFiles() = runTest(testScheduler) {
        val ep1 = vmEpisode("ep1")
        val ep2 = vmEpisode("ep2")
        val fileStore = FakeEpisodeFileStore(
            mapOf(
                "ep1" to "file:///episodes/ep1.mp3",
                "ep2" to "file:///episodes/ep2.mp3"
            )
        )
        val player = FakeAudioPlayer()
        val vm = createViewModel(fileStore = fileStore, player = player)
        advanceUntilIdle()

        vm.playEpisode(ep1)
        advanceUntilIdle()

        vm.toggleDownloadEpisode(ep1)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.downloadError)
        assertTrue("ep1" in vm.uiState.value.downloadedEpisodeIds)
        assertFalse("ep1" in fileStore.deletedIds)

        vm.dismissDownloadError()
        assertNull(vm.uiState.value.downloadError)

        vm.toggleDownloadEpisode(ep2)
        advanceUntilIdle()

        assertFalse("ep2" in vm.uiState.value.downloadedEpisodeIds)
        assertTrue("ep2" in fileStore.deletedIds)
    }

    @Test
    fun removeSubscriptionCleansNotificationsAndOrphanedDownloadsWhileKeepingFavorites() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val removed = vmPodcast("https://x/removed", "Removed", "ep-orphan", "ep-fav")
        val kept = vmPodcast("https://x/kept", "Kept", "ep-kept")
        store.saveSubscriptions(listOf(removed, kept))
        store.saveFavorites(listOf(removed.episodes[1]))
        store.saveNotifications(
            listOf(
                NewEpisodeNotification(removed.episodes[0], "https://x/removed"),
                NewEpisodeNotification(kept.episodes[0], "https://x/kept")
            )
        )

        val fileStore = FakeEpisodeFileStore(
            mapOf(
                "ep-orphan" to "file:///episodes/ep-orphan.mp3",
                "ep-fav" to "file:///episodes/ep-fav.mp3",
                "ep-kept" to "file:///episodes/ep-kept.mp3"
            )
        )
        val vm = createViewModel(storage = storage, fileStore = fileStore)
        advanceUntilIdle()

        vm.removeSubscription("https://x/removed")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(kept), state.subscriptions)
        assertEquals(listOf("ep-kept"), state.notifications.map { it.episode.id })
        assertEquals(setOf("ep-fav", "ep-kept"), state.downloadedEpisodeIds)
        assertEquals(listOf("ep-orphan"), fileStore.deletedIds)
    }

    @Test
    fun searchQueryDebouncesAndSkipsLiveSearchForFeedUrls() = runTest(testScheduler) {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            requestedUrls += url
            if (url.contains("itunes.apple.com")) {
                respond(
                    content = """
                        {"results":[{"collectionName":"Kotlin Show","artistName":"JetBrains","feedUrl":"https://example.com/feed.xml"}]}
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = """
                        <rss><channel>
                          <title>Example Feed</title>
                          <item><title>Ep 1</title><guid>g1</guid><enclosure url="https://example.com/1.mp3"/></item>
                        </channel></rss>
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/xml")
                )
            }
        })

        val vm = createViewModel(httpClient = client)
        advanceUntilIdle()

        vm.updateSearchQuery("kot")
        advanceTimeBy(399)
        runCurrent()
        assertTrue(requestedUrls.isEmpty())

        advanceTimeBy(2)
        awaitCondition { !vm.uiState.value.isSearching && vm.uiState.value.searchResults.isNotEmpty() }
        assertNull(vm.uiState.value.searchError)
        assertEquals(1, requestedUrls.size)
        assertEquals(listOf("Kotlin Show"), vm.uiState.value.searchResults.map { it.title })

        vm.updateSearchQuery("https://example.com/feed.xml")
        advanceTimeBy(1_000)
        advanceUntilIdle()
        assertEquals(1, requestedUrls.size)

        vm.searchPodcasts()
        awaitCondition { !vm.uiState.value.isAddingRss && vm.uiState.value.subscriptions.isNotEmpty() }

        assertEquals(2, requestedUrls.size)
        assertEquals("Example Feed", vm.uiState.value.subscriptions.single().title)
        assertEquals(AppTab.LIBRARY, vm.uiState.value.activeTab)
        assertEquals(LibrarySection.SUBSCRIPTIONS, vm.uiState.value.librarySection)
    }

    @Test
    fun toggleFavoriteQueueAndPlayedPersistChanges() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val sub = vmPodcast("https://x/p", "Podcast", "ep1", "ep2")
        val ep1 = sub.episodes[0]
        val ep2 = sub.episodes[1]
        store.saveSubscriptions(listOf(sub))
        store.savePositions(mapOf("ep1" to 30_000L))

        val player = FakeAudioPlayer()
        val vm = createViewModel(storage = storage, player = player)
        advanceUntilIdle()

        vm.playEpisode(ep1)
        advanceUntilIdle()

        vm.toggleQueue(ep1) // su an caldigi icin eklenmez
        vm.toggleQueue(ep2)
        vm.toggleFavorite(ep2)
        vm.togglePlayed(ep1)
        vm.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(ep2), state.queue)
        assertEquals(listOf(ep2), state.favorites)
        assertTrue("ep1" in state.playedEpisodeIds)
        assertFalse("ep1" in state.episodePositions)
        assertEquals(ThemeMode.LIGHT, state.themeMode)

        val reloaded = PodcastStore(storage)
        assertEquals(listOf(ep2), reloaded.loadQueue())
        assertEquals(listOf(ep2), reloaded.loadFavorites())
        assertTrue("ep1" in reloaded.loadPlayedEpisodeIds())
        assertFalse("ep1" in reloaded.loadPositions())
        assertEquals(ThemeMode.LIGHT, reloaded.loadThemeMode())
    }

    @Test
    fun bookmarksAddPlayAtPositionAndDeletePersist() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val ep1 = vmEpisode("ep1", "Studio Pod")
        store.saveSubscriptions(listOf(vmPodcast("https://x/p", "Studio Pod", "ep1")))

        val player = FakeAudioPlayer()
        val vm = createViewModel(storage = storage, player = player)
        advanceUntilIdle()

        vm.playEpisode(ep1)
        advanceUntilIdle()
        player.emitPosition(95_000L)
        advanceUntilIdle()

        vm.addBookmark("Onemli nokta")
        advanceUntilIdle()

        val created = vm.uiState.value.bookmarks.single()
        assertEquals(95_000L, created.positionMs)
        assertEquals("Onemli nokta", created.note)
        assertEquals(listOf(created), PodcastStore(storage).loadBookmarks())

        vm.playBookmark(created)
        advanceUntilIdle()
        assertEquals(ep1 to 95_000L, player.played.last())

        vm.deleteBookmark(created.id)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.bookmarks.isEmpty())
        assertTrue(PodcastStore(storage).loadBookmarks().isEmpty())
    }

    @Test
    fun listeningStatsTrackPlaybackAndCompletionAndClearWithHistory() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val ep1 = vmEpisode("ep1", "Bilim Cast")
        store.saveSubscriptions(listOf(vmPodcast("https://x/p", "Bilim Cast", "ep1")))

        val player = FakeAudioPlayer()
        val vm = createViewModel(storage = storage, player = player)
        advanceUntilIdle()

        vm.playEpisode(ep1)
        advanceUntilIdle()
        player.emitPosition(4_000L)
        advanceUntilIdle()
        player.emitPosition(8_000L)
        advanceUntilIdle()
        player.emitPosition(12_000L)
        advanceUntilIdle()
        player.emitEnded()
        advanceUntilIdle()

        val stats = vm.uiState.value.listeningStats
        assertEquals(12L, stats.totalListeningSeconds)
        assertEquals(1, stats.completedEpisodesCount)
        assertEquals(1, stats.activeDayKeys.size)
        assertEquals(12L, stats.podcastListeningSeconds["Bilim Cast"])
        assertEquals(stats, PodcastStore(storage).loadListeningStats())

        vm.clearListeningHistory()
        advanceUntilIdle()
        assertEquals(ListeningStats(), vm.uiState.value.listeningStats)
        assertEquals(ListeningStats(), PodcastStore(storage).loadListeningStats())
    }

    @Test
    fun themePaletteSpeedAndSkipIntervalsPersist() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val player = FakeAudioPlayer()
        val vm = createViewModel(storage = storage, player = player)
        advanceUntilIdle()

        vm.setThemePalette(ThemePalette.OCEAN)
        vm.setPlaybackSpeed(1.5f)
        vm.setSkipBackSeconds(5)
        vm.setSkipForwardSeconds(45)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ThemePalette.OCEAN, state.themePalette)
        assertEquals(5, state.skipBackSeconds)
        assertEquals(45, state.skipForwardSeconds)
        assertEquals(1.5f, player.state.value.playbackSpeed)

        val reloaded = PodcastStore(storage)
        assertEquals(ThemePalette.OCEAN, reloaded.loadThemePalette())
        assertEquals(1.5f, reloaded.loadPlaybackSpeed())
        assertEquals(5, reloaded.loadSkipBackSeconds())
        assertEquals(45, reloaded.loadSkipForwardSeconds())

        val secondPlayer = FakeAudioPlayer()
        val reopenedVm = createViewModel(storage = storage, player = secondPlayer)
        advanceUntilIdle()
        assertEquals(ThemePalette.OCEAN, reopenedVm.uiState.value.themePalette)
        assertEquals(1.5f, secondPlayer.state.value.playbackSpeed)
    }

    @Test
    fun opmlImportAddsNewFeedsAndSkipsExisting() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val existingSub = vmPodcast("https://existing.example.com/rss", "Existing", "old-1")
        store.saveSubscriptions(listOf(existingSub))

        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requestedUrls += request.url.toString()
            respond(
                content = """
                    <rss><channel>
                      <title>Yeni OPML Yayini</title>
                      <item><title>Bolum A</title><guid>new-1</guid><enclosure url="https://example.com/a.mp3"/></item>
                    </channel></rss>
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/xml")
            )
        })

        val vm = createViewModel(storage = storage, httpClient = client)
        advanceUntilIdle()

        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <body>
                <outline type="rss" text="Existing" xmlUrl="https://existing.example.com/rss" />
                <outline type="rss" text="Yeni" xmlUrl="https://new.example.com/rss" />
              </body>
            </opml>
        """.trimIndent()

        vm.importOpml(opml)
        awaitCondition { !vm.uiState.value.isImportingOpml && vm.uiState.value.subscriptions.size == 2 }

        assertEquals(listOf("https://new.example.com/rss"), requestedUrls)
        assertEquals(
            setOf("https://existing.example.com/rss", "https://new.example.com/rss"),
            vm.uiState.value.subscriptions.map { it.feedUrl }.toSet()
        )
        assertEquals(2, PodcastStore(storage).loadSubscriptions().size)
    }

    @Test
    fun listeningHistoryRecordsPlayProgressCompletionRemovalAndClear() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val sub = vmPodcast("https://x/p", "Bilim Cast", "ep1", "ep2")
        val ep1 = sub.episodes[0]
        val ep2 = sub.episodes[1]
        store.saveSubscriptions(listOf(sub))

        val player = FakeAudioPlayer()
        val vm = createViewModel(storage = storage, player = player)
        advanceUntilIdle()

        vm.playEpisode(ep1)
        advanceUntilIdle()

        val firstEntry = vm.uiState.value.listeningHistory.single()
        assertEquals("ep1", firstEntry.episode.id)
        assertEquals(1, firstEntry.playCount)
        assertFalse(firstEntry.completed)

        player.emitPosition(4_000L)
        advanceUntilIdle()
        player.emitPosition(8_000L)
        advanceUntilIdle()
        player.emitPosition(12_000L)
        advanceUntilIdle()

        val afterProgress = vm.uiState.value.listeningHistory.single()
        assertEquals(12_000L, afterProgress.lastPositionMs)
        assertEquals(12L, afterProgress.totalListenedSeconds)
        assertEquals(12L, vm.uiState.value.listeningStats.dailyListeningSeconds.values.sum())

        vm.playEpisode(ep2)
        advanceUntilIdle()
        vm.playEpisode(ep1)
        advanceUntilIdle()

        assertEquals(listOf("ep1", "ep2"), vm.uiState.value.listeningHistory.map { it.episode.id })
        assertEquals(2, vm.uiState.value.listeningHistory.first().playCount)

        player.emitEnded()
        advanceUntilIdle()

        val completedEntry = vm.uiState.value.listeningHistory.first()
        assertTrue(completedEntry.completed)
        assertEquals(completedEntry, PodcastStore(storage).loadListeningHistory().first())

        vm.removeFromListeningHistory("ep2")
        advanceUntilIdle()
        assertEquals(listOf("ep1"), vm.uiState.value.listeningHistory.map { it.episode.id })
        assertEquals(listOf("ep1"), vm.uiState.value.recentEpisodes.map { it.id })
        assertEquals(1, PodcastStore(storage).loadListeningHistory().size)

        vm.clearListeningHistory()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.listeningHistory.isEmpty())
        assertTrue(vm.uiState.value.recentEpisodes.isEmpty())
        assertEquals(ListeningStats(), vm.uiState.value.listeningStats)
        assertTrue(PodcastStore(storage).loadListeningHistory().isEmpty())
    }

    @Test
    fun notificationsDismissSingleAndClearAllPersist() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val ep1 = vmEpisode("ep1", "Pod A")
        val ep2 = vmEpisode("ep2", "Pod B")
        store.saveNotifications(
            listOf(
                NewEpisodeNotification(episode = ep1, podcastFeedUrl = "https://x/a", isRead = false),
                NewEpisodeNotification(episode = ep2, podcastFeedUrl = "https://x/b", isRead = true)
            )
        )

        val vm = createViewModel(storage = storage)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.notifications.size)

        vm.dismissNotification("ep1")
        advanceUntilIdle()
        assertEquals(listOf("ep2"), vm.uiState.value.notifications.map { it.episode.id })
        assertEquals(listOf("ep2"), PodcastStore(storage).loadNotifications().map { it.episode.id })

        vm.clearNotifications()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.notifications.isEmpty())
        assertNull(vm.uiState.value.refreshMessage)
        assertEquals("Bildirimler temizlendi", vm.uiState.value.statusMessage)
        assertTrue(PodcastStore(storage).loadNotifications().isEmpty())
    }

    @Test
    fun listeningHistoryMigratesLegacyRecentEpisodesWhenHistoryFileIsEmpty() = runTest(testScheduler) {
        val storage = FakeTextStorage()
        val store = PodcastStore(storage)
        val ep1 = vmEpisode("ep1", "Eski Pod")
        val ep2 = vmEpisode("ep2", "Eski Pod")
        store.saveRecentEpisodes(listOf(ep1, ep2))
        store.savePositions(mapOf("ep1" to 42_000L))
        store.savePlayedEpisodeIds(setOf("ep2"))

        val vm = createViewModel(storage = storage)
        advanceUntilIdle()

        val migrated = vm.uiState.value.listeningHistory
        assertEquals(2, migrated.size)
        assertEquals("ep1", migrated[0].episode.id)
        assertEquals(42_000L, migrated[0].lastPositionMs)
        assertEquals(600_000L, migrated[0].durationMs)
        assertFalse(migrated[0].completed)
        assertEquals(0L, migrated[0].lastPlayedAtMs)

        assertEquals("ep2", migrated[1].episode.id)
        assertTrue(migrated[1].completed)
    }

    private suspend fun TestScope.awaitCondition(predicate: () -> Boolean) {
        repeat(200) {
            advanceUntilIdle()
            if (predicate()) return
            withContext(Dispatchers.Default) { delay(10) }
        }
        assertTrue(predicate(), "Kosul zamaninda saglanmadi")
    }
}
