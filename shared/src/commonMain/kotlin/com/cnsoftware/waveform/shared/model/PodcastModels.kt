package com.cnsoftware.waveform.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Podcast(
    val feedUrl: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String?,
    val episodes: List<Episode>
)

@Serializable
data class Episode(
    val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val pubDate: String,
    val durationText: String,
    val imageUrl: String?,
    val podcastTitle: String
)

data class SearchPodcastResult(
    val title: String,
    val author: String,
    val feedUrl: String,
    val artworkUrl: String?
)

@Serializable
data class NewEpisodeNotification(
    val episode: Episode,
    val podcastFeedUrl: String,
    val isRead: Boolean = false
)

@Serializable
data class EpisodeBookmark(
    val id: String,
    val episode: Episode,
    val positionMs: Long,
    val note: String = "",
    val createdAtMs: Long = 0L
)

@Serializable
data class ListeningStats(
    val totalListeningSeconds: Long = 0L,
    val completedEpisodesCount: Int = 0,
    val activeDayKeys: Set<String> = emptySet(),
    val podcastListeningSeconds: Map<String, Long> = emptyMap(),
    val dailyListeningSeconds: Map<String, Long> = emptyMap()
)

@Serializable
data class ListeningHistoryEntry(
    val episode: Episode,
    val lastPlayedAtMs: Long,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val totalListenedSeconds: Long = 0L,
    val playCount: Int = 1,
    val completed: Boolean = false
)

data class PlayerState(
    val currentEpisode: Episode? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    /** Gecerli bolum sonuna kadar calindiysa true; yeni bolum baslayinca ya da basa sarinca false. */
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val errorMessage: String? = null
)
