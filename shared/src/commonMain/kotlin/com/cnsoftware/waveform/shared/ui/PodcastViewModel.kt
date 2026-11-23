package com.cnsoftware.waveform.shared.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnsoftware.waveform.shared.data.BackgroundRefreshScheduler
import com.cnsoftware.waveform.shared.data.EpisodeFileStore
import com.cnsoftware.waveform.shared.data.PlatformEpisodeFileStore
import com.cnsoftware.waveform.shared.data.PodcastRepository
import com.cnsoftware.waveform.shared.data.PodcastStore
import com.cnsoftware.waveform.shared.data.RefreshScheduler
import com.cnsoftware.waveform.shared.data.fetchAll
import com.cnsoftware.waveform.shared.data.reconcileRefresh
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.EpisodeBookmark
import com.cnsoftware.waveform.shared.model.ListeningHistoryEntry
import com.cnsoftware.waveform.shared.model.ListeningStats
import com.cnsoftware.waveform.shared.model.NewEpisodeNotification
import com.cnsoftware.waveform.shared.model.PlayerState
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.model.SearchPodcastResult
import com.cnsoftware.waveform.shared.player.AudioPlayer
import com.cnsoftware.waveform.shared.player.PodcastAudioPlayer
import com.cnsoftware.waveform.shared.rss.OpmlCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.time.TimeSource

enum class AppTab {
    HOME,
    DISCOVER,
    LIBRARY,
    NOTIFICATIONS,
    SETTINGS,
    HISTORY
}

enum class LibrarySection {
    SUBSCRIPTIONS,
    DOWNLOADS,
    FAVORITES,
    BOOKMARKS
}

data class PodcastUiState(
    /** Kayitli veriler diskten okunana kadar true; bos durum ekranlarini yanlislikla gostermemek icin. */
    val isLoading: Boolean = true,
    val activeTab: AppTab = AppTab.HOME,
    val librarySection: LibrarySection = LibrarySection.SUBSCRIPTIONS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themePalette: ThemePalette = ThemePalette.STUDIO,
    val subscriptions: List<Podcast> = emptyList(),
    val recentEpisodes: List<Episode> = emptyList(),
    val notifications: List<NewEpisodeNotification> = emptyList(),
    val episodePositions: Map<String, Long> = emptyMap(),
    val downloadedEpisodeIds: Set<String> = emptySet(),
    val downloadingEpisodeIds: Set<String> = emptySet(),
    /** 0f..1f; toplam boyut bilinmiyorsa bolum icin kayit yoktur (belirsiz ilerleme). */
    val downloadProgress: Map<String, Float> = emptyMap(),
    val downloadError: String? = null,
    val statusMessage: String? = null,
    /** Zamanli uyku zamanlayicisinda kalan sure; zamanlayici yoksa null. */
    val sleepTimerRemainingMs: Long? = null,
    /** "Bolum sonunda durdur" modu aciksa true. */
    val sleepAtEndOfEpisode: Boolean = false,
    /** Bolum bitince, listede bir sonraki (daha yeni) bolumu otomatik cal. */
    val autoPlayNext: Boolean = true,
    val skipBackSeconds: Int = 10,
    val skipForwardSeconds: Int = 30,
    /** Calma sirasi: en ustteki bolum siradaki bolumdur. */
    val queue: List<Episode> = emptyList(),
    /** Favori bolumler; en son eklenen basta. */
    val favorites: List<Episode> = emptyList(),
    /** Zaman damgali yer imleri; en son eklenen basta. */
    val bookmarks: List<EpisodeBookmark> = emptyList(),
    val listeningStats: ListeningStats = ListeningStats(),
    val listeningHistory: List<ListeningHistoryEntry> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val playedEpisodeIds: Set<String> = emptySet(),
    val backgroundRefreshEnabled: Boolean = true,
    val selectedPodcast: Podcast? = null,
    val isPlayerExpanded: Boolean = false,
    val isRefreshingSubscriptions: Boolean = false,
    val refreshMessage: String? = null,
    val isAddRssDialogVisible: Boolean = false,
    val isAddingRss: Boolean = false,
    val isImportingOpml: Boolean = false,
    val addingFeedUrl: String? = null,
    val rssError: String? = null,
    val searchQuery: String = "",
    val searchResults: List<SearchPodcastResult> = emptyList(),
    val hasSearched: Boolean = false,
    val isSearching: Boolean = false,
    val searchError: String? = null
)

/**
 * Feed'ler yeniden eskiye sirali oldugu icin "sonraki" bolum, listede bir onceki (daha yeni)
 * girdidir. En yeni bolumde ya da bolum aboneliklerde bulunamazsa null doner.
 */
internal fun findNextEpisode(
    subscriptions: List<Podcast>,
    current: Episode,
    playedIds: Set<String> = emptySet()
): Episode? {
    val podcast = subscriptions.firstOrNull { p ->
        p.title == current.podcastTitle && p.episodes.any { it.id == current.id }
    } ?: subscriptions.firstOrNull { p -> p.episodes.any { it.id == current.id } }
        ?: return null
    val index = podcast.episodes.indexOfFirst { it.id == current.id }
    // Daha yeni bolumlere dogru ilerle, zaten dinlenmis olanlari atla.
    return (index - 1 downTo 0)
        .map { podcast.episodes[it] }
        .firstOrNull { it.id !in playedIds }
}

/** Bosluk icermeyen, http(s) ile baslayan metin RSS adresi sayilir. */
internal fun looksLikeFeedUrl(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.none { it.isWhitespace() } &&
        (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) &&
        trimmed.length > "https://".length
}

/**
 * Bir abonelik silindiginde baska hicbir listede (diger abonelikler, favoriler, sira, son dinlenenler,
 * yer imleri) ya da aktif yerel oynatmada yer almayan, artik sahipsiz kalmis indirme kimliklerini bulur.
 */
internal fun findOrphanedDownloadIds(
    removedPodcast: Podcast,
    remainingSubscriptions: List<Podcast>,
    favorites: List<Episode>,
    queue: List<Episode>,
    recentEpisodes: List<Episode>,
    downloadedIds: Set<String>,
    playingLocalId: String?,
    bookmarkedEpisodes: List<Episode> = emptyList()
): Set<String> {
    val removedIds = removedPodcast.episodes.mapTo(HashSet()) { it.id }
    val candidates = downloadedIds.intersect(removedIds)
    if (candidates.isEmpty()) return emptySet()
    val retainedIds = buildSet {
        remainingSubscriptions.forEach { p -> p.episodes.forEach { add(it.id) } }
        favorites.forEach { add(it.id) }
        queue.forEach { add(it.id) }
        recentEpisodes.forEach { add(it.id) }
        bookmarkedEpisodes.forEach { add(it.id) }
        if (playingLocalId != null) add(playingLocalId)
    }
    return candidates - retainedIds
}

class PodcastViewModel(
    private val repository: PodcastRepository = PodcastRepository(),
    private val store: PodcastStore = PodcastStore(),
    private val episodeFileStore: EpisodeFileStore = PlatformEpisodeFileStore(),
    private val audioPlayer: AudioPlayer = PodcastAudioPlayer(),
    private val backgroundRefresh: RefreshScheduler = BackgroundRefreshScheduler(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    // Tema ve palet küçük dosyalardan eşzamanlı okunur: ilk karede yanlış tema parlamasın.
    private val _uiState = MutableStateFlow(
        PodcastUiState(
            themeMode = store.loadThemeMode(),
            themePalette = store.loadThemePalette()
        )
    )
    val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = audioPlayer.state
    val isBackgroundRefreshSupported: Boolean = backgroundRefresh.isSupported

    // Tek serili yazma kuyrugu: ard arda disk yazimlarinda eski durum yenisinin ustune yazmasin.
    private val persistDispatcher = ioDispatcher.limitedParallelism(1)
    private var sleepTimerJob: Job? = null
    private var playJob: Job? = null
    /** Surmekte olan indirmeler (bolum kimligi -> is); iptal icin. Yalnizca ana is parcaciginda kullanilir. */
    private val downloadJobs = mutableMapOf<String, Job>()
    private var endHandledEpisodeId: String? = null
    private var lastPersistedEpisodeId: String? = null
    private var lastPersistedPositionMs: Long = 0L

    private var statsTrackedEpisodeId: String? = null
    private var statsLastPositionMs: Long = 0L
    private var pendingListeningMs: Long = 0L
    private var pendingPodcastTitle: String = ""
    private var pendingEpisodeId: String? = null

    init {
        loadPersistedState()
        observePlayerProgress()
    }

    private fun persistAsync(block: () -> Unit) {
        viewModelScope.launch(persistDispatcher) { block() }
    }

    private fun playingLocalEpisodeId(): String? =
        audioPlayer.state.value.currentEpisode
            ?.takeIf { it.audioUrl.startsWith("file:") }
            ?.id

    private fun loadPersistedState() {
        viewModelScope.launch {
            var backgroundEnabled = true
            try {
                // Dosyalari sirayla degil paralel oku; en buyuk dosya (abonelikler) toplam sureyi belirler.
                coroutineScope {
                    val subscriptions = async(ioDispatcher) { store.loadSubscriptions() }
                    val positions = async(ioDispatcher) { store.loadPositions() }
                    val notifications = async(ioDispatcher) { store.loadNotifications() }
                    val recent = async(ioDispatcher) { store.loadRecentEpisodes() }
                    val downloaded = async(ioDispatcher) { episodeFileStore.listDownloadedIds() }
                    val autoPlayNext = async(ioDispatcher) { store.loadAutoPlayNext() }
                    val queue = async(ioDispatcher) { store.loadQueue() }
                    val favorites = async(ioDispatcher) { store.loadFavorites() }
                    val bookmarks = async(ioDispatcher) { store.loadBookmarks() }
                    val listeningStats = async(ioDispatcher) { store.loadListeningStats() }
                    val listeningHistory = async(ioDispatcher) { store.loadListeningHistory() }
                    val recentSearches = async(ioDispatcher) { store.loadRecentSearches() }
                    val played = async(ioDispatcher) { store.loadPlayedEpisodeIds() }
                    val background = async(ioDispatcher) { store.loadBackgroundRefresh() }
                    val savedSpeed = async(ioDispatcher) { store.loadPlaybackSpeed() }
                    val skipBack = async(ioDispatcher) { store.loadSkipBackSeconds() }
                    val skipForward = async(ioDispatcher) { store.loadSkipForwardSeconds() }

                    backgroundEnabled = background.await()
                    val speed = savedSpeed.await()
                    if (speed != 1.0f) {
                        audioPlayer.setPlaybackSpeed(speed)
                    }
                    val loadedRecent = recent.await()
                    val loadedPositions = positions.await()
                    val loadedPlayed = played.await()
                    val loadedHistory = listeningHistory.await()
                    val resolvedHistory = if (loadedHistory.isEmpty() && loadedRecent.isNotEmpty()) {
                        loadedRecent.map { ep ->
                            ListeningHistoryEntry(
                                episode = ep,
                                lastPlayedAtMs = 0L,
                                lastPositionMs = loadedPositions[ep.id] ?: 0L,
                                durationMs = parseDurationMs(ep.durationText) ?: 0L,
                                completed = ep.id in loadedPlayed
                            )
                        }
                    } else {
                        loadedHistory
                    }
                    _uiState.update {
                        it.copy(
                            backgroundRefreshEnabled = backgroundEnabled,
                            playedEpisodeIds = loadedPlayed,
                            autoPlayNext = autoPlayNext.await(),
                            skipBackSeconds = skipBack.await(),
                            skipForwardSeconds = skipForward.await(),
                            queue = queue.await(),
                            favorites = favorites.await(),
                            bookmarks = bookmarks.await(),
                            listeningStats = listeningStats.await(),
                            listeningHistory = resolvedHistory,
                            recentSearches = recentSearches.await(),
                            subscriptions = subscriptions.await(),
                            recentEpisodes = loadedRecent,
                            episodePositions = loadedPositions,
                            notifications = notifications.await(),
                            downloadedEpisodeIds = downloaded.await()
                        )
                    }
                }
            } finally {
                // Okuma basarisiz olsa bile arayuz sonsuza dek "yukleniyor"da kalmasin.
                _uiState.update { it.copy(isLoading = false) }
            }
            // Ilk cizimi geciktirmemek icin zamanlayiciyi yukleme bittikten sonra ayarla.
            backgroundRefresh.setEnabled(backgroundEnabled)
        }
    }

    /**
     * Arka plan yenilemesi dosyalari uygulama disinda guncellemis olabilir; one gelince abonelik
     * ve bildirimleri diskten tazele. Kullanici yenilemesi sürerken dokunma (kendi sonucunu yazacak).
     */
    fun reloadFromStore() {
        // Ilk yukleme suruyorken ayni dosyalari ikinci kez cozmeye gerek yok.
        if (_uiState.value.isLoading || _uiState.value.isRefreshingSubscriptions) return
        viewModelScope.launch {
            val subscriptions = withContext(ioDispatcher) { store.loadSubscriptions() }
            val notifications = withContext(ioDispatcher) { store.loadNotifications() }
            _uiState.update { state ->
                if (state.isRefreshingSubscriptions) {
                    state
                } else {
                    state.copy(
                        subscriptions = subscriptions,
                        notifications = notifications,
                        selectedPodcast = subscriptions.firstOrNull {
                            it.feedUrl == state.selectedPodcast?.feedUrl
                        } ?: state.selectedPodcast
                    )
                }
            }
        }
    }

    fun setBackgroundRefresh(enabled: Boolean) {
        backgroundRefresh.setEnabled(enabled)
        _uiState.update { it.copy(backgroundRefreshEnabled = enabled) }
        persistAsync { store.saveBackgroundRefresh(enabled) }
        if (enabled) requestNotificationPermissionIfNeeded()
    }

    private fun observePlayerProgress() {
        viewModelScope.launch {
            audioPlayer.state.collect { state ->
                trackListeningStatsDelta(state)
                handleEpisodeEnded(state)
                stopAtEndOfEpisodeIfRequested(state)
                if (state.isEnded) return@collect
                val episodeId = state.currentEpisode?.id ?: return@collect
                val pos = state.positionMs
                if (pos <= 0L) return@collect

                val episodeChanged = episodeId != lastPersistedEpisodeId
                val movedEnough = abs(pos - lastPersistedPositionMs) >= 3000L
                val justPaused = !state.isPlaying && pos != lastPersistedPositionMs

                if (episodeChanged || movedEnough || justPaused) {
                    lastPersistedEpisodeId = episodeId
                    lastPersistedPositionMs = pos
                    var historyUpdated = false
                    val after = _uiState.updateAndGet { current ->
                        historyUpdated = false
                        val duration = state.durationMs
                        val idx = current.listeningHistory.indexOfFirst { it.episode.id == episodeId }
                        val updatedHistory = if (idx >= 0) {
                            val entry = current.listeningHistory[idx]
                            val nextDuration = duration.takeIf { it > 0L } ?: entry.durationMs
                            if (entry.lastPositionMs != pos || entry.durationMs != nextDuration) {
                                historyUpdated = true
                                current.listeningHistory.toMutableList().apply {
                                    this[idx] = entry.copy(
                                        lastPositionMs = pos,
                                        durationMs = nextDuration,
                                        lastPlayedAtMs = nowMillis()
                                    )
                                }
                            } else {
                                current.listeningHistory
                            }
                        } else {
                            current.listeningHistory
                        }
                        current.copy(
                            episodePositions = current.episodePositions + (episodeId to pos),
                            listeningHistory = updatedHistory
                        )
                    }
                    persistAsync {
                        store.savePositions(after.episodePositions)
                        if (historyUpdated) store.saveListeningHistory(after.listeningHistory)
                    }
                }
            }
        }
    }

    private fun trackListeningStatsDelta(state: PlayerState) {
        val episode = state.currentEpisode ?: return
        if (episode.id != statsTrackedEpisodeId) {
            flushListeningDelta(pendingEpisodeId, pendingPodcastTitle)
            statsTrackedEpisodeId = episode.id
            statsLastPositionMs = state.positionMs
            pendingEpisodeId = episode.id
            pendingPodcastTitle = episode.podcastTitle
        } else {
            if (state.isPlaying) {
                val delta = state.positionMs - statsLastPositionMs
                if (delta in 1L..5_000L) {
                    pendingListeningMs += delta
                }
            }
            statsLastPositionMs = state.positionMs
        }
        val shouldFlush = pendingListeningMs >= 10_000L ||
            ((!state.isPlaying || state.isEnded) && pendingListeningMs >= 1_000L)
        if (shouldFlush) {
            flushListeningDelta(episode.id, episode.podcastTitle)
        }
    }

    private fun flushListeningDelta(episodeId: String?, podcastTitle: String) {
        val wholeSeconds = pendingListeningMs / 1000L
        if (wholeSeconds <= 0L) return
        pendingListeningMs %= 1000L
        val now = nowMillis()
        val today = localDayKey(now)
        var historyUpdated = false
        val after = _uiState.updateAndGet { current ->
            historyUpdated = false
            val prev = current.listeningStats
            val updatedByPodcast = if (podcastTitle.isNotBlank()) {
                prev.podcastListeningSeconds + (
                    podcastTitle to ((prev.podcastListeningSeconds[podcastTitle] ?: 0L) + wholeSeconds)
                )
            } else {
                prev.podcastListeningSeconds
            }
            val updatedDaily = prev.dailyListeningSeconds + (
                today to ((prev.dailyListeningSeconds[today] ?: 0L) + wholeSeconds)
            )
            val updatedHistory = if (episodeId != null) {
                val idx = current.listeningHistory.indexOfFirst { it.episode.id == episodeId }
                if (idx >= 0) {
                    historyUpdated = true
                    val entry = current.listeningHistory[idx]
                    current.listeningHistory.toMutableList().apply {
                        this[idx] = entry.copy(
                            totalListenedSeconds = entry.totalListenedSeconds + wholeSeconds,
                            lastPlayedAtMs = now
                        )
                    }
                } else {
                    current.listeningHistory
                }
            } else {
                current.listeningHistory
            }
            current.copy(
                listeningStats = prev.copy(
                    totalListeningSeconds = prev.totalListeningSeconds + wholeSeconds,
                    activeDayKeys = prev.activeDayKeys + today,
                    podcastListeningSeconds = updatedByPodcast,
                    dailyListeningSeconds = updatedDaily
                ),
                listeningHistory = updatedHistory
            )
        }
        persistAsync {
            store.saveListeningStats(after.listeningStats)
            if (historyUpdated) store.saveListeningHistory(after.listeningHistory)
        }
    }

    private fun handleEpisodeEnded(state: PlayerState) {
        val episode = state.currentEpisode ?: return
        if (!state.isEnded) {
            endHandledEpisodeId = null
            return
        }
        // Ayni bitis icin yinelenen durum yayinlarinda bir kez calis.
        if (endHandledEpisodeId == episode.id) return
        endHandledEpisodeId = episode.id

        flushListeningDelta(episode.id, episode.podcastTitle)
        val now = nowMillis()
        val today = localDayKey(now)
        var historyUpdated = false
        val statsAfter = _uiState.updateAndGet { current ->
            historyUpdated = false
            val prev = current.listeningStats
            val idx = current.listeningHistory.indexOfFirst { it.episode.id == episode.id }
            val updatedHistory = if (idx >= 0) {
                historyUpdated = true
                val entry = current.listeningHistory[idx]
                val nextDuration = state.durationMs.takeIf { it > 0L } ?: entry.durationMs
                val nextPosition = state.durationMs.takeIf { it > 0L } ?: entry.lastPositionMs
                current.listeningHistory.toMutableList().apply {
                    this[idx] = entry.copy(
                        completed = true,
                        lastPlayedAtMs = now,
                        durationMs = nextDuration,
                        lastPositionMs = nextPosition
                    )
                }
            } else {
                current.listeningHistory
            }
            current.copy(
                listeningStats = prev.copy(
                    completedEpisodesCount = prev.completedEpisodesCount + 1,
                    activeDayKeys = prev.activeDayKeys + today
                ),
                listeningHistory = updatedHistory
            )
        }
        persistAsync {
            store.saveListeningStats(statsAfter.listeningStats)
            if (historyUpdated) store.saveListeningHistory(statsAfter.listeningHistory)
        }

        // Biten bolum dinlendi sayilir ve bir sonraki acilista sondan degil bastan baslar.
        lastPersistedEpisodeId = episode.id
        lastPersistedPositionMs = 0L
        setEpisodesPlayed(listOf(episode.id), played = true)

        // "Bolum sonunda uyu" secildiyse sonraki bolume gecme.
        if (_uiState.value.sleepAtEndOfEpisode) {
            _uiState.update { it.copy(sleepAtEndOfEpisode = false) }
            return
        }
        // Kuyruk acik bir kullanici tercihidir: "otomatik sonraki" kapali olsa bile once o calinir.
        val next = _uiState.value.queue.firstOrNull()
            ?: if (_uiState.value.autoPlayNext) {
                findNextEpisode(_uiState.value.subscriptions, episode, _uiState.value.playedEpisodeIds)
            } else {
                null
            }
        next?.let { playEpisode(it) }
    }

    fun togglePlayed(episode: Episode) {
        setEpisodesPlayed(listOf(episode.id), played = episode.id !in _uiState.value.playedEpisodeIds)
    }

    fun setPodcastPlayed(podcast: Podcast, played: Boolean) {
        setEpisodesPlayed(podcast.episodes.map { it.id }, played)
    }

    /** Dinlendi olarak isaretlenen bolumlerin kayitli konumu silinir; bir sonraki acilista bastan baslar. */
    private fun setEpisodesPlayed(episodeIds: List<String>, played: Boolean) {
        val idSet = episodeIds.toSet()
        var positionsChanged = false
        var historyChanged = false
        val after = _uiState.updateAndGet { state ->
            positionsChanged = false
            historyChanged = false
            val updatedPlayed = if (played) state.playedEpisodeIds + idSet else state.playedEpisodeIds - idSet
            val updatedPositions = if (played) state.episodePositions - idSet else state.episodePositions
            positionsChanged = updatedPositions != state.episodePositions
            val updatedHistory = state.listeningHistory.map { entry ->
                if (entry.episode.id in idSet && entry.completed != played) {
                    historyChanged = true
                    val resetEndPos = !played && entry.durationMs > 0L && entry.lastPositionMs >= entry.durationMs
                    entry.copy(
                        completed = played,
                        lastPositionMs = if (resetEndPos) 0L else entry.lastPositionMs
                    )
                } else {
                    entry
                }
            }
            state.copy(
                playedEpisodeIds = updatedPlayed,
                episodePositions = updatedPositions,
                listeningHistory = updatedHistory
            )
        }
        persistAsync {
            store.savePlayedEpisodeIds(after.playedEpisodeIds)
            if (positionsChanged) store.savePositions(after.episodePositions)
            if (historyChanged) store.saveListeningHistory(after.listeningHistory)
        }
    }

    fun toggleQueue(episode: Episode) {
        val state = _uiState.value
        // Su an calan bolumu siraya eklemenin anlami yok.
        if (episode.id == audioPlayer.state.value.currentEpisode?.id) return
        val inQueue = state.queue.any { it.id == episode.id }
        setQueue(
            if (inQueue) PlayQueue.remove(state.queue, episode.id)
            else PlayQueue.addLast(state.queue, episode)
        )
    }

    fun toggleFavorite(episode: Episode) {
        val after = _uiState.updateAndGet { state ->
            val current = state.favorites
            val updated = if (current.any { it.id == episode.id }) {
                current.filterNot { it.id == episode.id }
            } else {
                listOf(episode) + current
            }
            state.copy(favorites = updated)
        }
        persistAsync { store.saveFavorites(after.favorites) }
    }

    fun addBookmark(note: String = "") {
        val player = audioPlayer.state.value
        val episode = player.currentEpisode ?: return
        val posMs = player.positionMs.coerceAtLeast(0L)
        val now = nowMillis()
        val bookmark = EpisodeBookmark(
            id = "${episode.id}@$posMs@$now",
            episode = episode,
            positionMs = posMs,
            note = note.trim(),
            createdAtMs = now
        )
        val after = _uiState.updateAndGet { state ->
            state.copy(
                bookmarks = listOf(bookmark) + state.bookmarks,
                statusMessage = "Yer imi kaydedildi (${formatBookmarkClock(posMs)})"
            )
        }
        persistAsync { store.saveBookmarks(after.bookmarks) }
    }

    fun deleteBookmark(bookmarkId: String) {
        val after = _uiState.updateAndGet { state ->
            state.copy(bookmarks = state.bookmarks.filterNot { it.id == bookmarkId })
        }
        persistAsync { store.saveBookmarks(after.bookmarks) }
    }

    fun playBookmark(bookmark: EpisodeBookmark) {
        playEpisodeAt(bookmark.episode, bookmark.positionMs)
    }

    fun removeFromQueue(episodeId: String) {
        setQueue(PlayQueue.remove(_uiState.value.queue, episodeId))
    }

    fun moveDownInQueue(episodeId: String) {
        setQueue(PlayQueue.moveDown(_uiState.value.queue, episodeId))
    }

    fun moveUpInQueue(episodeId: String) {
        setQueue(PlayQueue.moveUp(_uiState.value.queue, episodeId))
    }

    fun clearQueue() {
        setQueue(emptyList())
    }

    private fun setQueue(updated: List<Episode>) {
        if (updated == _uiState.value.queue) return
        _uiState.update { it.copy(queue = updated) }
        persistAsync { store.saveQueue(updated) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _uiState.update { it.copy(autoPlayNext = enabled) }
        persistAsync { store.saveAutoPlayNext(enabled) }
    }

    fun setSkipBackSeconds(seconds: Int) {
        val safe = seconds.coerceIn(1, 120)
        _uiState.update { it.copy(skipBackSeconds = safe) }
        persistAsync { store.saveSkipBackSeconds(safe) }
    }

    fun setSkipForwardSeconds(seconds: Int) {
        val safe = seconds.coerceIn(1, 120)
        _uiState.update { it.copy(skipForwardSeconds = safe) }
        persistAsync { store.saveSkipForwardSeconds(safe) }
    }

    private fun stopAtEndOfEpisodeIfRequested(state: PlayerState) {
        if (!_uiState.value.sleepAtEndOfEpisode) return
        val nearEnd = state.durationMs > 0L && state.positionMs >= state.durationMs - END_OF_EPISODE_MARGIN_MS
        if (!nearEnd) return
        if (state.isPlaying) audioPlayer.togglePlayPause()
        _uiState.update { it.copy(sleepAtEndOfEpisode = false) }
    }

    fun startSleepTimer(minutes: Int) {
        require(minutes > 0) { "Süre pozitif olmalı." }
        cancelSleepTimer()
        val totalMs = minutes * 60_000L
        _uiState.update { it.copy(sleepTimerRemainingMs = totalMs) }
        sleepTimerJob = viewModelScope.launch {
            val startedAt = TimeSource.Monotonic.markNow()
            while (true) {
                val remaining = totalMs - startedAt.elapsedNow().inWholeMilliseconds
                if (remaining <= 0L) break
                _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
                delay(minOf(1_000L, remaining))
            }
            if (audioPlayer.state.value.isPlaying) audioPlayer.togglePlayPause()
            _uiState.update { it.copy(sleepTimerRemainingMs = null) }
        }
    }

    fun sleepAtEndOfEpisode() {
        cancelSleepTimer()
        _uiState.update { it.copy(sleepAtEndOfEpisode = true) }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _uiState.update { it.copy(sleepTimerRemainingMs = null, sleepAtEndOfEpisode = false) }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab, selectedPodcast = null) }
    }

    fun selectLibrarySection(section: LibrarySection) {
        _uiState.update {
            it.copy(
                activeTab = AppTab.LIBRARY,
                librarySection = section,
                selectedPodcast = null
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        persistAsync { store.saveThemeMode(mode) }
    }

    fun setThemePalette(palette: ThemePalette) {
        _uiState.update { it.copy(themePalette = palette) }
        persistAsync { store.saveThemePalette(palette) }
    }

    /**
     * Tum indirilen bolumleri siler. Su an yerel dosyadan caliyorsa o bolum atlanir; calarken
     * dosyayi silmek oynaticida hataya yol acar.
     */
    fun deleteAllDownloads() {
        val playingLocalId = playingLocalEpisodeId()
        val toDelete = _uiState.value.downloadedEpisodeIds - setOfNotNull(playingLocalId)
        if (toDelete.isEmpty()) return
        viewModelScope.launch {
            withContext(ioDispatcher) { toDelete.forEach { episodeFileStore.deleteAudio(it) } }
            _uiState.update { it.copy(downloadedEpisodeIds = it.downloadedEpisodeIds - toDelete) }
        }
    }

    fun removeFromListeningHistory(episodeId: String) {
        val after = _uiState.updateAndGet { state ->
            state.copy(
                listeningHistory = state.listeningHistory.filterNot { it.episode.id == episodeId },
                recentEpisodes = state.recentEpisodes.filterNot { it.id == episodeId },
                statusMessage = "Geçmişten kaldırıldı"
            )
        }
        persistAsync {
            store.saveListeningHistory(after.listeningHistory)
            store.saveRecentEpisodes(after.recentEpisodes)
        }
    }

    /** Dinlendi isaretlerini, kayitli konumlari, istatistikleri ve dinleme gecmisini temizler. */
    fun clearListeningHistory() {
        pendingListeningMs = 0L
        _uiState.update {
            it.copy(
                playedEpisodeIds = emptySet(),
                episodePositions = emptyMap(),
                recentEpisodes = emptyList(),
                listeningHistory = emptyList(),
                listeningStats = ListeningStats(),
                statusMessage = "Dinleme geçmişi temizlendi"
            )
        }
        lastPersistedEpisodeId = null
        lastPersistedPositionMs = 0L
        persistAsync {
            store.savePlayedEpisodeIds(emptySet())
            store.savePositions(emptyMap())
            store.saveRecentEpisodes(emptyList())
            store.saveListeningHistory(emptyList())
            store.saveListeningStats(ListeningStats())
        }
    }

    fun refreshSubscriptions() {
        val currentSubs = _uiState.value.subscriptions
        if (currentSubs.isEmpty() || _uiState.value.isRefreshingSubscriptions) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isRefreshingSubscriptions = true, refreshMessage = null)
            }

            val results = try {
                repository.fetchAll(currentSubs)
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isRefreshingSubscriptions = false) }
                throw e
            } catch (e: Exception) {
                // Beklenmeyen hata yenileme göstergesini sonsuza dek açık bırakmasın.
                _uiState.update {
                    it.copy(
                        isRefreshingSubscriptions = false,
                        refreshMessage = "Yenileme başarısız oldu"
                    )
                }
                return@launch
            }

            val after = _uiState.updateAndGet { state ->
                val outcome = reconcileRefresh(
                    results = results,
                    latestSubscriptions = state.subscriptions,
                    currentNotifications = state.notifications
                )
                val selectedFeedUrl = state.selectedPodcast?.feedUrl
                val updatedSelected = outcome.subscriptions.firstOrNull { it.feedUrl == selectedFeedUrl }
                val newCount = outcome.newNotifications.size
                state.copy(
                    subscriptions = outcome.subscriptions,
                    notifications = outcome.notifications,
                    selectedPodcast = updatedSelected ?: state.selectedPodcast,
                    isRefreshingSubscriptions = false,
                    refreshMessage = if (newCount > 0) {
                        "$newCount yeni bölüm bulundu"
                    } else {
                        "Tüm abonelikler güncel"
                    }
                )
            }

            persistAsync {
                store.saveSubscriptions(after.subscriptions)
                store.saveNotifications(after.notifications)
            }
        }
    }

    fun markNotificationRead(episodeId: String) {
        val after = _uiState.updateAndGet { state ->
            val updated = state.notifications.map { item ->
                if (item.episode.id == episodeId) item.copy(isRead = true) else item
            }
            state.copy(notifications = updated)
        }
        persistAsync { store.saveNotifications(after.notifications) }
    }

    fun markAllNotificationsRead() {
        val after = _uiState.updateAndGet { state ->
            state.copy(notifications = state.notifications.map { it.copy(isRead = true) })
        }
        persistAsync { store.saveNotifications(after.notifications) }
    }

    fun dismissNotification(episodeId: String) {
        var changed = false
        val after = _uiState.updateAndGet { state ->
            changed = false
            val updated = state.notifications.filterNot { it.episode.id == episodeId }
            changed = updated.size != state.notifications.size
            state.copy(notifications = updated)
        }
        if (changed) {
            persistAsync { store.saveNotifications(after.notifications) }
        }
    }

    fun clearNotifications() {
        if (_uiState.value.notifications.isEmpty()) return
        _uiState.update {
            it.copy(
                notifications = emptyList(),
                refreshMessage = null,
                statusMessage = "Bildirimler temizlendi"
            )
        }
        persistAsync { store.saveNotifications(emptyList()) }
    }

    fun openPodcastDetail(podcast: Podcast) {
        _uiState.update { it.copy(selectedPodcast = podcast) }
    }

    fun closePodcastDetail() {
        _uiState.update { it.copy(selectedPodcast = null) }
    }

    fun showAddRssDialog() {
        _uiState.update { it.copy(isAddRssDialogVisible = true, rssError = null) }
    }

    fun hideAddRssDialog() {
        _uiState.update { it.copy(isAddRssDialogVisible = false, rssError = null) }
    }

    fun addPodcastByRss(rssUrl: String, openAfterAdd: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAddingRss = true,
                    addingFeedUrl = rssUrl,
                    rssError = null,
                    searchError = null
                )
            }
            repository.fetchPodcastByRss(rssUrl)
                .onSuccess { podcast ->
                    val after = _uiState.updateAndGet { state ->
                        val updated = listOf(podcast) + state.subscriptions.filterNot {
                            it.feedUrl == podcast.feedUrl
                        }
                        state.copy(
                            subscriptions = updated,
                            isAddingRss = false,
                            isSearching = false,
                            addingFeedUrl = null,
                            isAddRssDialogVisible = false,
                            selectedPodcast = if (openAfterAdd) podcast else state.selectedPodcast,
                            activeTab = AppTab.LIBRARY,
                            librarySection = LibrarySection.SUBSCRIPTIONS
                        )
                    }
                    persistAsync { store.saveSubscriptions(after.subscriptions) }
                    if (after.backgroundRefreshEnabled) requestNotificationPermissionIfNeeded()
                }
                .onFailure { error ->
                    val message = error.message ?: "RSS beslemesi eklenemedi."
                    _uiState.update {
                        it.copy(
                            isAddingRss = false,
                            isSearching = false,
                            addingFeedUrl = null,
                            rssError = message,
                            searchError = if (it.activeTab == AppTab.DISCOVER) message else it.searchError
                        )
                    }
                }
        }
    }

    fun removeSubscription(feedUrl: String) {
        val stateSnapshot = _uiState.value
        val removedPodcast = stateSnapshot.subscriptions.firstOrNull { it.feedUrl == feedUrl } ?: return
        val remainingSubs = stateSnapshot.subscriptions.filterNot { it.feedUrl == feedUrl }
        val playingLocalId = playingLocalEpisodeId()
        val bookmarkedEpisodes = stateSnapshot.bookmarks.map { it.episode }
        val orphanedDownloads = findOrphanedDownloadIds(
            removedPodcast = removedPodcast,
            remainingSubscriptions = remainingSubs,
            favorites = stateSnapshot.favorites,
            queue = stateSnapshot.queue,
            recentEpisodes = stateSnapshot.recentEpisodes,
            downloadedIds = stateSnapshot.downloadedEpisodeIds,
            playingLocalId = playingLocalId,
            bookmarkedEpisodes = bookmarkedEpisodes
        )
        val retainedIds = buildSet {
            remainingSubs.forEach { p -> p.episodes.forEach { add(it.id) } }
            stateSnapshot.favorites.forEach { add(it.id) }
            stateSnapshot.queue.forEach { add(it.id) }
            stateSnapshot.recentEpisodes.forEach { add(it.id) }
            bookmarkedEpisodes.forEach { add(it.id) }
        }
        removedPodcast.episodes.forEach { ep ->
            if (ep.id !in retainedIds) downloadJobs[ep.id]?.cancel()
        }

        val after = _uiState.updateAndGet { state ->
            val nextRemaining = state.subscriptions.filterNot { it.feedUrl == feedUrl }
            val nextNotifications = state.notifications.filterNot { it.podcastFeedUrl == feedUrl }
            state.copy(
                subscriptions = nextRemaining,
                notifications = nextNotifications,
                downloadedEpisodeIds = state.downloadedEpisodeIds - orphanedDownloads,
                selectedPodcast = state.selectedPodcast?.takeIf { it.feedUrl != feedUrl }
            )
        }
        persistAsync {
            store.saveSubscriptions(after.subscriptions)
            store.saveNotifications(after.notifications)
        }
        if (orphanedDownloads.isNotEmpty()) {
            viewModelScope.launch(ioDispatcher) {
                orphanedDownloads.forEach { episodeFileStore.deleteAudio(it) }
            }
        }
    }

    fun exportOpml(): String = OpmlCodec.export(_uiState.value.subscriptions)

    fun importOpml(opmlXml: String) {
        if (_uiState.value.isImportingOpml) return
        val urls = OpmlCodec.parseFeedUrls(opmlXml)
        if (urls.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Geçerli OPML besleme bağlantısı bulunamadı.") }
            return
        }
        val existingUrls = _uiState.value.subscriptions.map { it.feedUrl }.toSet()
        val missingUrls = urls.filterNot { it in existingUrls }
        if (missingUrls.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "OPML içindeki tüm abonelikler zaten ekli.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingOpml = true) }
            val imported = try {
                coroutineScope {
                    missingUrls.map { url ->
                        async { repository.fetchPodcastByRss(url).getOrNull() }
                    }.awaitAll().filterNotNull()
                }
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isImportingOpml = false) }
                throw cancelled
            }
            if (imported.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isImportingOpml = false,
                        statusMessage = "OPML beslemeleri yüklenemedi."
                    )
                }
                return@launch
            }
            val after = _uiState.updateAndGet { state ->
                val importedUrls = imported.map { it.feedUrl }.toSet()
                val merged = imported + state.subscriptions.filterNot { it.feedUrl in importedUrls }
                state.copy(
                    subscriptions = merged,
                    isImportingOpml = false,
                    statusMessage = "${imported.size} podcast OPML'den içe aktarıldı"
                )
            }
            persistAsync { store.saveSubscriptions(after.subscriptions) }
        }
    }

    private var searchJob: Job? = null

    /**
     * Yazdikca arama: her tus vurusunda onceki arama iptal edilir, kisa bir sessizlikten sonra
     * (debounce) iTunes'da aranir. Baglanti yapistirilirsa otomatik aranmaz (Enter/Ara ile eklenir).
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    hasSearched = false,
                    isSearching = false,
                    searchError = null
                )
            }
            return
        }
        if (trimmed.length < MIN_LIVE_SEARCH_LENGTH || looksLikeFeedUrl(trimmed)) {
            _uiState.update { it.copy(isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(trimmed)
        }
    }

    /** Elle arama (Enter / Ara dugmesi): beklemeden hemen calisir. */
    fun searchPodcasts() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return

        // Aramaya yapistirilan bir baglanti ise iTunes'da aramak yerine dogrudan RSS olarak ekle.
        if (looksLikeFeedUrl(query)) {
            searchJob?.cancel()
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            addPodcastByRss(query.trim(), openAfterAdd = true)
            return
        }

        val trimmed = query.trim()
        recordRecentSearch(trimmed)
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(trimmed) }
    }

    fun searchByCategory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                activeTab = AppTab.DISCOVER,
                selectedPodcast = null,
                searchQuery = trimmed,
                searchError = null
            )
        }
        recordRecentSearch(trimmed)
        searchJob = viewModelScope.launch { runSearch(trimmed) }
    }

    fun removeRecentSearch(query: String) {
        val after = _uiState.updateAndGet { state ->
            state.copy(recentSearches = state.recentSearches.filterNot { it == query })
        }
        persistAsync { store.saveRecentSearches(after.recentSearches) }
    }

    fun clearRecentSearches() {
        _uiState.update { it.copy(recentSearches = emptyList()) }
        persistAsync { store.saveRecentSearches(emptyList()) }
    }

    private fun recordRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_LIVE_SEARCH_LENGTH || looksLikeFeedUrl(trimmed)) return
        val after = _uiState.updateAndGet { state ->
            val updated = (listOf(trimmed) + state.recentSearches.filterNot {
                it.equals(trimmed, ignoreCase = true)
            }).take(MAX_RECENT_SEARCHES)
            state.copy(recentSearches = updated)
        }
        persistAsync { store.saveRecentSearches(after.recentSearches) }
    }

    private suspend fun runSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, searchError = null, hasSearched = true) }
        val result = repository.searchPodcasts(query)
        // Iptal edilen (eskimis) arama sonucu yeni sonucun ustune yazilmasin.
        currentCoroutineContext().ensureActive()
        result
            .onSuccess { results ->
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = error.message ?: "Arama sırasında hata oluştu."
                    )
                }
            }
    }

    fun openPlayer() {
        if (audioPlayer.state.value.currentEpisode != null) {
            _uiState.update { it.copy(isPlayerExpanded = true) }
        }
    }

    fun closePlayer() {
        _uiState.update { it.copy(isPlayerExpanded = false) }
    }

    fun playEpisode(episode: Episode) {
        startPlayback(episode, explicitPositionMs = null)
    }

    fun playEpisodeAt(episode: Episode, positionMs: Long) {
        startPlayback(episode, explicitPositionMs = positionMs.coerceAtLeast(0L))
    }

    private fun startPlayback(episode: Episode, explicitPositionMs: Long?) {
        // "Bolum sonu" modu, secildigi bolume ozeldir; baska bolume gecince sonraki bolumu etkilemesin.
        if (audioPlayer.state.value.currentEpisode?.id != episode.id) {
            _uiState.update { it.copy(sleepAtEndOfEpisode = false) }
        }
        // Calinmaya baslayan bolum siradan dusulur (elle ya da otomatik fark etmez).
        setQueue(PlayQueue.remove(_uiState.value.queue, episode.id))
        markNotificationRead(episode.id)
        val switchingEpisode = audioPlayer.state.value.currentEpisode?.id != episode.id
        val playedAt = nowMillis()
        var targetPosition = 0L
        val after = _uiState.updateAndGet { state ->
            targetPosition = explicitPositionMs ?: (state.episodePositions[episode.id] ?: 0L)
            val updatedRecent = (listOf(episode) + state.recentEpisodes.filterNot {
                it.id == episode.id
            }).take(10)
            val existing = state.listeningHistory.firstOrNull { it.episode.id == episode.id }
            val updatedEntry = ListeningHistoryEntry(
                episode = episode,
                lastPlayedAtMs = playedAt,
                lastPositionMs = targetPosition,
                durationMs = existing?.durationMs?.takeIf { it > 0L }
                    ?: (parseDurationMs(episode.durationText) ?: 0L),
                totalListenedSeconds = existing?.totalListenedSeconds ?: 0L,
                playCount = if (existing == null) {
                    1
                } else if (switchingEpisode) {
                    existing.playCount + 1
                } else {
                    existing.playCount
                },
                completed = if (explicitPositionMs == 0L) {
                    false
                } else {
                    existing?.completed ?: (episode.id in state.playedEpisodeIds)
                }
            )
            val updatedHistory = (listOf(updatedEntry) + state.listeningHistory.filterNot {
                it.episode.id == episode.id
            }).take(MAX_HISTORY_ENTRIES)
            state.copy(
                recentEpisodes = updatedRecent,
                listeningHistory = updatedHistory
            )
        }
        persistAsync {
            store.saveRecentEpisodes(after.recentEpisodes)
            store.saveListeningHistory(after.listeningHistory)
        }

        playJob?.cancel()
        playJob = viewModelScope.launch {
            val localUri = withContext(ioDispatcher) { episodeFileStore.getLocalFileUri(episode.id) }
            val playableEpisode = if (localUri != null) episode.copy(audioUrl = localUri) else episode
            audioPlayer.play(playableEpisode, startPositionMs = targetPosition)
        }
    }

    fun toggleDownloadEpisode(episode: Episode) {
        val state = _uiState.value
        // Indirme suruyorsa ayni dokunus iptaldir; yarim dosya depoda temizlenir.
        if (episode.id in state.downloadingEpisodeIds) {
            downloadJobs[episode.id]?.cancel()
            return
        }

        if (episode.id in state.downloadedEpisodeIds) {
            if (episode.id == playingLocalEpisodeId()) {
                _uiState.update {
                    it.copy(downloadError = "Şu an çalınan indirilen bölüm silinemez.")
                }
                return
            }
            viewModelScope.launch {
                withContext(ioDispatcher) {
                    episodeFileStore.deleteAudio(episode.id)
                }
                _uiState.update {
                    it.copy(downloadedEpisodeIds = it.downloadedEpisodeIds - episode.id)
                }
            }
            return
        }

        downloadJobs[episode.id] = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloadingEpisodeIds = it.downloadingEpisodeIds + episode.id,
                    downloadError = null
                )
            }
            var lastReportedPercent = -1
            val result = try {
                val sinkResult = runCatching {
                    withContext(ioDispatcher) { episodeFileStore.openAudioSink(episode.id, episode.audioUrl) }
                }
                sinkResult.fold(
                    onSuccess = { sink ->
                        repository.downloadAudio(episode.audioUrl, sink) { downloaded, total ->
                            if (total == null) return@downloadAudio
                            val percent = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            // Her parcada degil, yalnizca yuzde degistiginde durumu guncelle.
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                _uiState.update {
                                    it.copy(
                                        downloadProgress = it.downloadProgress + (episode.id to percent / 100f)
                                    )
                                }
                            }
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            } finally {
                // Iptal edilse bile (kullanici ya da ViewModel temizlenmesi) durum tutarli kalsin.
                downloadJobs.remove(episode.id)
                _uiState.update {
                    it.copy(
                        downloadingEpisodeIds = it.downloadingEpisodeIds - episode.id,
                        downloadProgress = it.downloadProgress - episode.id
                    )
                }
            }
            // Iptal edildiyse sonuc/hata islemeye gecme: iptal bir hata degildir.
            ensureActive()
            result
                .onSuccess {
                    _uiState.update { it.copy(downloadedEpisodeIds = it.downloadedEpisodeIds + episode.id) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            downloadError = "\"${episode.title}\" indirilemedi: " +
                                (error.message ?: "bilinmeyen hata")
                        )
                    }
                }
        }
    }

    fun dismissDownloadError() {
        _uiState.update { it.copy(downloadError = null) }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun togglePlayPause() {
        audioPlayer.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        val duration = audioPlayer.state.value.durationMs
        val clamped = if (duration > 0L) {
            positionMs.coerceIn(0L, duration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        audioPlayer.seekTo(clamped)
    }

    fun seekBy(deltaMs: Long) {
        seekTo(audioPlayer.state.value.positionMs + deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
        persistAsync { store.savePlaybackSpeed(speed) }
    }

    private fun formatBookmarkClock(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')
        return if (hours > 0L) "$hours:$mm:$ss" else "$mm:$ss"
    }

    private companion object {
        const val END_OF_EPISODE_MARGIN_MS = 1_000L
        const val SEARCH_DEBOUNCE_MS = 400L
        const val MIN_LIVE_SEARCH_LENGTH = 2
        const val MAX_RECENT_SEARCHES = 8
        const val MAX_HISTORY_ENTRIES = 200
    }

    override fun onCleared() {
        repository.close()
        audioPlayer.release()
        super.onCleared()
    }
}
