package com.cnsoftware.waveform.shared.data

import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.EpisodeBookmark
import com.cnsoftware.waveform.shared.model.ListeningHistoryEntry
import com.cnsoftware.waveform.shared.model.ListeningStats
import com.cnsoftware.waveform.shared.model.NewEpisodeNotification
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.ui.ThemeMode
import com.cnsoftware.waveform.shared.ui.ThemePalette
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PodcastStore(
    private val storage: TextStorage = LocalStorage()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadSubscriptions(): List<Podcast> {
        val raw = storage.readText(SUBSCRIPTIONS_FILE) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Podcast>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveSubscriptions(subscriptions: List<Podcast>) {
        storage.writeText(SUBSCRIPTIONS_FILE, json.encodeToString(subscriptions))
    }

    fun loadPositions(): Map<String, Long> {
        val raw = storage.readText(POSITIONS_FILE) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Long>>(raw) }
            .getOrDefault(emptyMap())
    }

    fun savePositions(positions: Map<String, Long>) {
        storage.writeText(POSITIONS_FILE, json.encodeToString(positions))
    }

    fun loadNotifications(): List<NewEpisodeNotification> {
        val raw = storage.readText(NOTIFICATIONS_FILE) ?: return emptyList()
        return runCatching { json.decodeFromString<List<NewEpisodeNotification>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveNotifications(notifications: List<NewEpisodeNotification>) {
        storage.writeText(NOTIFICATIONS_FILE, json.encodeToString(notifications))
    }

    fun loadRecentEpisodes(): List<Episode> {
        val raw = storage.readText(RECENT_EPISODES_FILE) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Episode>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveRecentEpisodes(episodes: List<Episode>) {
        storage.writeText(RECENT_EPISODES_FILE, json.encodeToString(episodes))
    }

    /** Kaydedilmemiş ya da bozuk değerde sistem temasını izler. */
    fun loadThemeMode(): ThemeMode =
        storage.readText(THEME_MODE_FILE)?.trim()
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM

    fun saveThemeMode(mode: ThemeMode) {
        storage.writeText(THEME_MODE_FILE, mode.name)
    }

    /** Kaydedilmemiş ya da bozuk değerde varsayılan Studio paletini kullanır. */
    fun loadThemePalette(): ThemePalette =
        storage.readText(THEME_PALETTE_FILE)?.trim()
            ?.let { saved -> ThemePalette.entries.firstOrNull { it.name == saved } }
            ?: ThemePalette.STUDIO

    fun saveThemePalette(palette: ThemePalette) {
        storage.writeText(THEME_PALETTE_FILE, palette.name)
    }

    /** Ayar hic kaydedilmediyse varsayilan olarak acik. */
    fun loadBackgroundRefresh(): Boolean =
        storage.readText(BACKGROUND_REFRESH_FILE)?.trim()?.toBooleanStrictOrNull() ?: true

    fun saveBackgroundRefresh(enabled: Boolean) {
        storage.writeText(BACKGROUND_REFRESH_FILE, enabled.toString())
    }

    fun loadPlayedEpisodeIds(): Set<String> {
        val raw = storage.readText(PLAYED_FILE) ?: return emptySet()
        return runCatching { json.decodeFromString<Set<String>>(raw) }
            .getOrDefault(emptySet())
    }

    fun savePlayedEpisodeIds(ids: Set<String>) {
        storage.writeText(PLAYED_FILE, json.encodeToString(ids))
    }

    fun loadQueue(): List<Episode> {
        val raw = storage.readText(QUEUE_FILE) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Episode>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveQueue(queue: List<Episode>) {
        storage.writeText(QUEUE_FILE, json.encodeToString(queue))
    }

    /** Favori bolumler (en son eklenen basta). Bolum bilgisi saklanir; abonelik birakilsa da kalir. */
    fun loadFavorites(): List<Episode> {
        val raw = storage.readText(FAVORITES_FILE) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Episode>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveFavorites(favorites: List<Episode>) {
        storage.writeText(FAVORITES_FILE, json.encodeToString(favorites))
    }

    fun loadBookmarks(): List<EpisodeBookmark> {
        val raw = storage.readText(BOOKMARKS_FILE) ?: return emptyList()
        return runCatching { json.decodeFromString<List<EpisodeBookmark>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveBookmarks(bookmarks: List<EpisodeBookmark>) {
        storage.writeText(BOOKMARKS_FILE, json.encodeToString(bookmarks))
    }

    fun loadListeningStats(): ListeningStats {
        val raw = storage.readText(LISTENING_STATS_FILE) ?: return ListeningStats()
        return runCatching { json.decodeFromString<ListeningStats>(raw) }
            .getOrDefault(ListeningStats())
    }

    fun saveListeningStats(stats: ListeningStats) {
        storage.writeText(LISTENING_STATS_FILE, json.encodeToString(stats))
    }

    fun loadListeningHistory(): List<ListeningHistoryEntry> {
        val raw = storage.readText(LISTENING_HISTORY_FILE) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ListeningHistoryEntry>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveListeningHistory(entries: List<ListeningHistoryEntry>) {
        storage.writeText(LISTENING_HISTORY_FILE, json.encodeToString(entries))
    }

    fun loadRecentSearches(): List<String> {
        val raw = storage.readText(RECENT_SEARCHES_FILE) ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveRecentSearches(queries: List<String>) {
        storage.writeText(RECENT_SEARCHES_FILE, json.encodeToString(queries))
    }

    fun loadPlaybackSpeed(): Float =
        storage.readText(PLAYBACK_SPEED_FILE)?.trim()?.toFloatOrNull()
            ?.takeIf { it.isFinite() }
            ?.coerceIn(0.5f, 3.0f)
            ?: 1.0f

    fun savePlaybackSpeed(speed: Float) {
        storage.writeText(PLAYBACK_SPEED_FILE, speed.coerceIn(0.5f, 3.0f).toString())
    }

    fun loadSkipBackSeconds(): Int =
        storage.readText(SKIP_BACK_FILE)?.trim()?.toIntOrNull()
            ?.takeIf { it in 1..120 }
            ?: 10

    fun saveSkipBackSeconds(seconds: Int) {
        storage.writeText(SKIP_BACK_FILE, seconds.coerceIn(1, 120).toString())
    }

    fun loadSkipForwardSeconds(): Int =
        storage.readText(SKIP_FORWARD_FILE)?.trim()?.toIntOrNull()
            ?.takeIf { it in 1..120 }
            ?: 30

    fun saveSkipForwardSeconds(seconds: Int) {
        storage.writeText(SKIP_FORWARD_FILE, seconds.coerceIn(1, 120).toString())
    }

    /** Ayar hic kaydedilmediyse varsayilan olarak acik. */
    fun loadAutoPlayNext(): Boolean =
        storage.readText(AUTO_PLAY_NEXT_FILE)?.trim()?.toBooleanStrictOrNull() ?: true

    fun saveAutoPlayNext(enabled: Boolean) {
        storage.writeText(AUTO_PLAY_NEXT_FILE, enabled.toString())
    }

    private companion object {
        const val AUTO_PLAY_NEXT_FILE = "auto_play_next.txt"
        const val BOOKMARKS_FILE = "bookmarks.json"
        const val LISTENING_HISTORY_FILE = "listening_history.json"
        const val LISTENING_STATS_FILE = "listening_stats.json"
        const val RECENT_SEARCHES_FILE = "recent_searches.json"
        const val PLAYBACK_SPEED_FILE = "playback_speed.txt"
        const val SKIP_BACK_FILE = "skip_back_seconds.txt"
        const val SKIP_FORWARD_FILE = "skip_forward_seconds.txt"
        const val THEME_PALETTE_FILE = "theme_palette.txt"
        const val QUEUE_FILE = "queue.json"
        const val FAVORITES_FILE = "favorites.json"
        const val BACKGROUND_REFRESH_FILE = "background_refresh.txt"
        const val THEME_MODE_FILE = "theme_mode.txt"
        const val PLAYED_FILE = "played_episodes.json"
        const val SUBSCRIPTIONS_FILE = "subscriptions.json"
        const val POSITIONS_FILE = "playback_positions.json"
        const val NOTIFICATIONS_FILE = "notifications.json"
        const val RECENT_EPISODES_FILE = "recent_episodes.json"
    }
}
