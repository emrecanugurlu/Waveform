@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.cnsoftware.waveform.shared.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.NewEpisodeNotification
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.ui.components.ConnectedToggleRow
import com.cnsoftware.waveform.shared.ui.components.HomeEpisodeCard
import com.cnsoftware.waveform.shared.ui.components.LiveWaveformBars
import com.cnsoftware.waveform.shared.ui.components.LocalFavorites
import com.cnsoftware.waveform.shared.ui.components.PodcastArtwork
import com.cnsoftware.waveform.shared.ui.components.ShapeBadge
import com.cnsoftware.waveform.shared.ui.components.formatTime
import com.cnsoftware.waveform.shared.ui.components.morphingPlayShape
import com.cnsoftware.waveform.shared.ui.episodeProgress
import com.cnsoftware.waveform.shared.ui.parseDurationMs

private enum class HomeEpisodeFilter {
    ALL,
    UNPLAYED,
    SHORT,
    DOWNLOADED
}

/** Ana sayfa: üst çubuktaki arama simgesiyle yazılan [query] abonelikler ve bölümler arasında aranır. */
@Composable
internal fun HomeScreen(
    query: String,
    subscriptions: List<Podcast>,
    recentEpisodes: List<Episode>,
    notifications: List<NewEpisodeNotification>,
    episodePositions: Map<String, Long>,
    downloadedEpisodeIds: Set<String>,
    downloadingEpisodeIds: Set<String>,
    downloadProgress: Map<String, Float>,
    queuedEpisodeIds: Set<String>,
    playedEpisodeIds: Set<String>,
    bookmarksCount: Int,
    historyCount: Int,
    skipForwardSeconds: Int,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onToggleQueue: (Episode) -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleDownload: (Episode) -> Unit,
    onSearchClick: () -> Unit,
    onAddRssClick: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val feed: @Composable () -> Unit = {
        HomeFeed(
            subscriptions = subscriptions,
            recentEpisodes = recentEpisodes,
            notifications = notifications,
            episodePositions = episodePositions,
            downloadedEpisodeIds = downloadedEpisodeIds,
            downloadingEpisodeIds = downloadingEpisodeIds,
            downloadProgress = downloadProgress,
            queuedEpisodeIds = queuedEpisodeIds,
            playedEpisodeIds = playedEpisodeIds,
            bookmarksCount = bookmarksCount,
            historyCount = historyCount,
            skipForwardSeconds = skipForwardSeconds,
            currentlyPlayingId = currentlyPlayingId,
            isPlaying = isPlaying,
            onToggleQueue = onToggleQueue,
            onOpenPodcast = onOpenPodcast,
            onPlayEpisode = onPlayEpisode,
            onTogglePlayPause = onTogglePlayPause,
            onSeekBy = onSeekBy,
            onToggleDownload = onToggleDownload,
            onSearchClick = onSearchClick,
            onAddRssClick = onAddRssClick,
            onOpenDownloads = onOpenDownloads,
            onOpenFavorites = onOpenFavorites,
            onOpenBookmarks = onOpenBookmarks,
            onOpenHistory = onOpenHistory,
            onOpenPlayer = onOpenPlayer
        )
    }

    if (subscriptions.isEmpty() && recentEpisodes.isEmpty()) {
        feed()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                feed()
            } else {
                val matchedPodcasts = remember(subscriptions, trimmed) {
                    subscriptions.filter {
                        it.title.contains(trimmed, ignoreCase = true) ||
                            it.author.contains(trimmed, ignoreCase = true)
                    }
                }
                val matchedEpisodes = remember(subscriptions, recentEpisodes, trimmed) {
                    (subscriptions.flatMap { it.episodes } + recentEpisodes)
                        .distinctBy { it.id }
                        .filter {
                            it.title.contains(trimmed, ignoreCase = true) ||
                                it.podcastTitle.contains(trimmed, ignoreCase = true)
                        }
                        .take(50)
                }
                if (matchedPodcasts.isEmpty() && matchedEpisodes.isEmpty()) {
                    Text(
                        text = "\"$trimmed\" için sonuç bulunamadı.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (matchedPodcasts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Podcastler",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            items(matchedPodcasts, key = { "p-" + it.feedUrl }) { podcast ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clickable { onOpenPodcast(podcast) },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PodcastArtwork(
                                            imageUrl = podcast.imageUrl,
                                            size = 54.dp,
                                            cornerRadius = 14.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = podcast.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${podcast.episodes.size} bölüm",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (matchedEpisodes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Bölümler",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            items(matchedEpisodes, key = { "e-" + it.id }) { episode ->
                                HomeEpisodeCard(
                                    episode = episode,
                                    episodePositions = episodePositions,
                                    downloadedEpisodeIds = downloadedEpisodeIds,
                                    downloadingEpisodeIds = downloadingEpisodeIds,
                                    downloadProgress = downloadProgress,
                                    queuedEpisodeIds = queuedEpisodeIds,
                                    currentlyPlayingId = currentlyPlayingId,
                                    isPlaying = isPlaying,
                                    onToggleQueue = onToggleQueue,
                                    onPlayEpisode = onPlayEpisode,
                                    onTogglePlayPause = onTogglePlayPause,
                                    onToggleDownload = onToggleDownload
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFeed(
    subscriptions: List<Podcast>,
    recentEpisodes: List<Episode>,
    notifications: List<NewEpisodeNotification>,
    episodePositions: Map<String, Long>,
    downloadedEpisodeIds: Set<String>,
    downloadingEpisodeIds: Set<String>,
    downloadProgress: Map<String, Float>,
    queuedEpisodeIds: Set<String>,
    playedEpisodeIds: Set<String>,
    bookmarksCount: Int,
    historyCount: Int,
    skipForwardSeconds: Int,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onToggleQueue: (Episode) -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleDownload: (Episode) -> Unit,
    onSearchClick: () -> Unit,
    onAddRssClick: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    if (subscriptions.isEmpty() && recentEpisodes.isEmpty()) {
        WelcomeCollage(
            onSearchClick = onSearchClick,
            onAddRssClick = onAddRssClick
        )
        return
    }

    var selectedFilter by rememberSaveable { mutableStateOf(HomeEpisodeFilter.ALL) }
    val favoritesState = LocalFavorites.current

    val latestEpisodes = remember(subscriptions, playedEpisodeIds, recentEpisodes, downloadedEpisodeIds, selectedFilter) {
        val recentIds = recentEpisodes.map { it.id }.toSet()
        val pool = subscriptions
            .flatMap { podcast ->
                podcast.episodes.filter { it.id !in recentIds }.take(6)
            }
            .distinctBy { it.id }

        val filtered = when (selectedFilter) {
            HomeEpisodeFilter.ALL -> pool.filter { it.id !in playedEpisodeIds }
            HomeEpisodeFilter.UNPLAYED -> pool.filter { it.id !in playedEpisodeIds }
            HomeEpisodeFilter.SHORT -> pool.filter { ep ->
                val ms = parseDurationMs(ep.durationText)
                ms != null && ms <= 25 * 60_000L
            }
            HomeEpisodeFilter.DOWNLOADED -> pool.filter { it.id in downloadedEpisodeIds }
        }
        filtered.take(15)
    }

    val downloadedEpisodes = remember(
        subscriptions, recentEpisodes, notifications, favoritesState.episodes, downloadedEpisodeIds
    ) {
        if (downloadedEpisodeIds.isEmpty()) {
            emptyList()
        } else {
            (subscriptions.flatMap { it.episodes } +
                recentEpisodes +
                favoritesState.episodes +
                notifications.map { it.episode })
                .distinctBy { it.id }
                .filter { it.id in downloadedEpisodeIds }
        }
    }
    val continueEpisode = recentEpisodes.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            QuickAccessRow(
                downloadsCount = downloadedEpisodeIds.size,
                favoritesCount = favoritesState.episodes.size,
                bookmarksCount = bookmarksCount,
                historyCount = historyCount,
                queueCount = queuedEpisodeIds.size,
                hasCurrentEpisode = currentlyPlayingId != null,
                onOpenDownloads = onOpenDownloads,
                onOpenFavorites = onOpenFavorites,
                onOpenBookmarks = onOpenBookmarks,
                onOpenHistory = onOpenHistory,
                onOpenPlayer = onOpenPlayer
            )
        }

        if (continueEpisode != null) {
            item {
                HeroContinueCard(
                    episode = continueEpisode,
                    savedMs = episodePositions[continueEpisode.id] ?: 0L,
                    isCurrent = continueEpisode.id == currentlyPlayingId,
                    isPlaying = isPlaying,
                    skipForwardSeconds = skipForwardSeconds,
                    onPlayOrToggle = {
                        if (continueEpisode.id == currentlyPlayingId) onTogglePlayPause()
                        else onPlayEpisode(continueEpisode)
                    },
                    onQuickForward = {
                        if (continueEpisode.id == currentlyPlayingId) {
                            onSeekBy(skipForwardSeconds * 1000L)
                        } else {
                            onPlayEpisode(continueEpisode)
                        }
                    }
                )
            }
        }

        if (subscriptions.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Abone Olduklarım",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val carouselState = rememberCarouselState { subscriptions.size }
                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        preferredItemWidth = 172.dp,
                        itemSpacing = 12.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(186.dp)
                    ) { index ->
                        val podcast = subscriptions[index]
                        val unplayedCount = podcast.episodes.count { it.id !in playedEpisodeIds }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .maskClip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onOpenPodcast(podcast) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Podcasts,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                if (!podcast.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = podcast.imageUrl,
                                        contentDescription = podcast.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.72f)
                                            )
                                        )
                                    )
                            )
                            if (unplayedCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "$unplayedCount yeni",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = podcast.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        if (favoritesState.episodes.isNotEmpty()) {
            item {
                HomeEpisodeShelf(
                    style = HomeShelfStyle.FAVORITE,
                    title = "Favoriler",
                    episodes = favoritesState.episodes,
                    episodePositions = episodePositions,
                    playedEpisodeIds = playedEpisodeIds,
                    queuedEpisodeIds = queuedEpisodeIds,
                    currentlyPlayingId = currentlyPlayingId,
                    isPlaying = isPlaying,
                    onSeeAll = onOpenFavorites,
                    onPlayEpisode = onPlayEpisode,
                    onTogglePlayPause = onTogglePlayPause,
                    onToggleQueue = onToggleQueue,
                    onSecondaryAction = { favoritesState.toggle(it) }
                )
            }
        }

        if (downloadedEpisodes.isNotEmpty()) {
            item {
                HomeEpisodeShelf(
                    style = HomeShelfStyle.OFFLINE,
                    title = "Çevrimdışı Hazır",
                    episodes = downloadedEpisodes,
                    episodePositions = episodePositions,
                    playedEpisodeIds = playedEpisodeIds,
                    queuedEpisodeIds = queuedEpisodeIds,
                    currentlyPlayingId = currentlyPlayingId,
                    isPlaying = isPlaying,
                    onSeeAll = onOpenDownloads,
                    onPlayEpisode = onPlayEpisode,
                    onTogglePlayPause = onTogglePlayPause,
                    onToggleQueue = onToggleQueue,
                    onSecondaryAction = onToggleDownload
                )
            }
        }

        if (recentEpisodes.size > 1) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "En Son Dinlediklerim",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = onOpenHistory,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Tümü")
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentEpisodes.drop(1), key = { it.id }) { episode ->
                            Card(
                                modifier = Modifier
                                    .width(142.dp)
                                    .clickable { onPlayEpisode(episode) },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    PodcastArtwork(
                                        imageUrl = episode.imageUrl,
                                        size = 122.dp,
                                        cornerRadius = 16.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = episode.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = episode.podcastTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Yeni Yayın Akışı",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                ConnectedToggleRow(
                    items = listOf(
                        HomeEpisodeFilter.ALL to "Tümü",
                        HomeEpisodeFilter.UNPLAYED to "Yeni",
                        HomeEpisodeFilter.SHORT to "Kısa",
                        HomeEpisodeFilter.DOWNLOADED to "İndirilen"
                    ),
                    selected = selectedFilter,
                    onSelect = { selectedFilter = it }
                )
            }
        }

        if (latestEpisodes.isEmpty()) {
            item {
                Text(
                    text = "Bu filtre için gösterilecek bölüm bulunmuyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(latestEpisodes, key = { it.id }) { episode ->
                HomeEpisodeCard(
                    episode = episode,
                    episodePositions = episodePositions,
                    downloadedEpisodeIds = downloadedEpisodeIds,
                    downloadingEpisodeIds = downloadingEpisodeIds,
                    downloadProgress = downloadProgress,
                    queuedEpisodeIds = queuedEpisodeIds,
                    currentlyPlayingId = currentlyPlayingId,
                    isPlaying = isPlaying,
                    onToggleQueue = onToggleQueue,
                    onPlayEpisode = onPlayEpisode,
                    onTogglePlayPause = onTogglePlayPause,
                    onToggleDownload = onToggleDownload
                )
            }
        }
    }
}

private enum class HomeShelfStyle {
    FAVORITE,
    OFFLINE
}

@Composable
private fun HomeEpisodeShelf(
    style: HomeShelfStyle,
    title: String,
    episodes: List<Episode>,
    episodePositions: Map<String, Long>,
    playedEpisodeIds: Set<String>,
    queuedEpisodeIds: Set<String>,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onSeeAll: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleQueue: (Episode) -> Unit,
    onSecondaryAction: (Episode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeShelfHeader(
            style = style,
            title = title,
            count = episodes.size,
            onSeeAll = onSeeAll
        )
        if (episodes.size == 1) {
            val episode = episodes.first()
            val isCurrent = episode.id == currentlyPlayingId
            HomeSpotlightEpisodeCard(
                style = style,
                episode = episode,
                savedMs = episodePositions[episode.id] ?: 0L,
                isPlayed = episode.id in playedEpisodeIds,
                isQueued = episode.id in queuedEpisodeIds,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                onPlayOrToggle = {
                    if (isCurrent) onTogglePlayPause() else onPlayEpisode(episode)
                },
                onToggleQueue = { onToggleQueue(episode) },
                onSecondaryAction = { onSecondaryAction(episode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth = (maxWidth - 52.dp).coerceIn(276.dp, 336.dp)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(episodes, key = { it.id }) { episode ->
                        val isCurrent = episode.id == currentlyPlayingId
                        HomeSpotlightEpisodeCard(
                            style = style,
                            episode = episode,
                            savedMs = episodePositions[episode.id] ?: 0L,
                            isPlayed = episode.id in playedEpisodeIds,
                            isQueued = episode.id in queuedEpisodeIds,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            onPlayOrToggle = {
                                if (isCurrent) onTogglePlayPause() else onPlayEpisode(episode)
                            },
                            onToggleQueue = { onToggleQueue(episode) },
                            onSecondaryAction = { onSecondaryAction(episode) },
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShelfHeader(
    style: HomeShelfStyle,
    title: String,
    count: Int,
    onSeeAll: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val iconShape = if (style == HomeShelfStyle.FAVORITE) {
        MaterialShapes.Cookie9Sided.toShape()
    } else {
        RoundedCornerShape(12.dp)
    }
    val iconContainer = if (style == HomeShelfStyle.FAVORITE) {
        colorScheme.tertiaryContainer
    } else {
        colorScheme.primaryContainer
    }
    val iconContent = if (style == HomeShelfStyle.FAVORITE) {
        colorScheme.onTertiaryContainer
    } else {
        colorScheme.onPrimaryContainer
    }
    val iconVector = if (style == HomeShelfStyle.FAVORITE) {
        Icons.Default.Favorite
    } else {
        Icons.Default.DownloadDone
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(iconShape)
                .background(iconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconContent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = iconContainer,
            contentColor = iconContent
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = onSeeAll,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("Tümü")
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HomeSpotlightEpisodeCard(
    style: HomeShelfStyle,
    episode: Episode,
    savedMs: Long,
    isPlayed: Boolean,
    isQueued: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayOrToggle: () -> Unit,
    onToggleQueue: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardGradient = if (style == HomeShelfStyle.FAVORITE) {
        listOf(
            colorScheme.tertiaryContainer.copy(alpha = 0.72f),
            colorScheme.surfaceContainerHigh
        )
    } else {
        listOf(
            colorScheme.primaryContainer.copy(alpha = 0.56f),
            colorScheme.surfaceContainerHigh
        )
    }
    val accentColor = if (style == HomeShelfStyle.FAVORITE) colorScheme.tertiary else colorScheme.primary
    val onAccentColor = if (style == HomeShelfStyle.FAVORITE) colorScheme.onTertiary else colorScheme.onPrimary
    val statusLabel = if (style == HomeShelfStyle.FAVORITE) "Favori" else "Çevrimdışı"
    val statusIcon = if (style == HomeShelfStyle.FAVORITE) Icons.Default.Favorite else Icons.Default.DownloadDone
    val secondaryIcon = if (style == HomeShelfStyle.FAVORITE) Icons.Default.Favorite else Icons.Default.DeleteOutline
    val secondaryDescription = if (style == HomeShelfStyle.FAVORITE) "Favorilerden çıkar" else "İndirilenlerden sil"
    val secondaryTint = if (style == HomeShelfStyle.FAVORITE) colorScheme.tertiary else colorScheme.onSurfaceVariant

    val progress = if (isPlayed) null else episodeProgress(episode.durationText, savedMs)
    val metaDetail = when {
        isPlayed -> "Dinlendi"
        savedMs > 0L -> "Kaldığın yer ${formatTime(savedMs)}"
        episode.durationText.isNotBlank() -> episode.durationText
        else -> null
    }
    val pillText = if (metaDetail != null) "$statusLabel • $metaDetail" else statusLabel
    val artworkShape = if (isCurrent && isPlaying) {
        MaterialShapes.Cookie12Sided.toShape()
    } else {
        RoundedCornerShape(18.dp)
    }

    Card(
        modifier = modifier.clickable(onClick = onPlayOrToggle),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isCurrent) 0.5f else 0.16f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = cardGradient))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PodcastArtwork(
                        imageUrl = episode.imageUrl,
                        size = 64.dp,
                        cornerRadius = 18.dp,
                        shape = artworkShape
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = episode.podcastTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onPlayOrToggle,
                        shape = morphingPlayShape(isCurrent && isPlaying),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = accentColor,
                            contentColor = onAccentColor
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Oynat / Duraklat"
                        )
                    }
                }

                if (progress != null) {
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        amplitude = { if (isCurrent && isPlaying) 1f else 0f },
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surface.copy(alpha = 0.72f),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isCurrent && isPlaying) {
                                LiveWaveformBars(
                                    isPlaying = true,
                                    barCount = 4,
                                    color = accentColor,
                                    modifier = Modifier.size(width = 16.dp, height = 11.dp)
                                )
                                Text(
                                    text = "Çalıyor",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = pillText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleQueue,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isQueued) {
                                    Icons.AutoMirrored.Filled.PlaylistAddCheck
                                } else {
                                    Icons.AutoMirrored.Filled.PlaylistAdd
                                },
                                contentDescription = if (isQueued) "Sıradan çıkar" else "Sıraya ekle",
                                tint = if (isQueued) accentColor else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onSecondaryAction,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = secondaryIcon,
                                contentDescription = secondaryDescription,
                                tint = secondaryTint,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessRow(
    downloadsCount: Int,
    favoritesCount: Int,
    bookmarksCount: Int,
    historyCount: Int,
    queueCount: Int,
    hasCurrentEpisode: Boolean,
    onOpenDownloads: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AssistChip(
                onClick = onOpenDownloads,
                label = { Text("İndirilenler ($downloadsCount)") },
                leadingIcon = {
                    Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
        item {
            AssistChip(
                onClick = onOpenFavorites,
                label = { Text("Favoriler ($favoritesCount)") },
                leadingIcon = {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
        item {
            AssistChip(
                onClick = onOpenBookmarks,
                label = { Text("Yer İmleri ($bookmarksCount)") },
                leadingIcon = {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
        item {
            AssistChip(
                onClick = onOpenHistory,
                label = { Text("Geçmiş ($historyCount)") },
                leadingIcon = {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
        if (hasCurrentEpisode || queueCount > 0) {
            item {
                AssistChip(
                    onClick = onOpenPlayer,
                    label = { Text("Sıra ($queueCount)") },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroContinueCard(
    episode: Episode,
    savedMs: Long,
    isCurrent: Boolean,
    isPlaying: Boolean,
    skipForwardSeconds: Int,
    onPlayOrToggle: () -> Unit,
    onQuickForward: () -> Unit
) {
    val progress = episodeProgress(episode.durationText, savedMs)

    BoxWithConstraints(modifier = Modifier.padding(horizontal = 16.dp)) {
        val compact = maxWidth < 360.dp
        val artSize = if (compact) 72.dp else 94.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlayOrToggle),
            shape = RoundedCornerShape(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    )
                    .padding(if (compact) 16.dp else 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PodcastArtwork(
                            imageUrl = episode.imageUrl,
                            size = artSize,
                            cornerRadius = 20.dp,
                            shape = MaterialShapes.Cookie12Sided.toShape()
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiveWaveformBars(
                                    isPlaying = isCurrent && isPlaying,
                                    barCount = 5,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(width = 22.dp, height = 14.dp)
                                )
                                Text(
                                    text = if (isCurrent && isPlaying) "ŞU AN ÇALIYOR" else "KALDIĞIN YERDEN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = buildString {
                                    append(episode.podcastTitle)
                                    if (savedMs > 0L) append(" • ${formatTime(savedMs)}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (progress != null) {
                        LinearWavyProgressIndicator(
                            progress = { progress },
                            amplitude = { if (isCurrent && isPlaying) 1f else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    SplitButtonLayout(
                        leadingButton = {
                            SplitButtonDefaults.LeadingButton(onClick = onPlayOrToggle) {
                                Icon(
                                    imageVector = if (isCurrent && isPlaying) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCurrent && isPlaying) "Duraklat" else "Devam Et")
                            }
                        },
                        trailingButton = {
                            SplitButtonDefaults.TrailingButton(onClick = onQuickForward) {
                                Icon(
                                    imageVector = Icons.Default.Forward30,
                                    contentDescription = "+$skipForwardSeconds sn",
                                    modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeCollage(
    onSearchClick: () -> Unit,
    onAddRssClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(176.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(64.dp)
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp)
                    .clip(MaterialShapes.Clover4Leaf.toShape())
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
            ShapeBadge(
                icon = Icons.Default.GraphicEq,
                shape = MaterialShapes.Cookie12Sided.toShape(),
                size = 128.dp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Sesin Ritmini Keşfet",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Favori yayıncılarını takip et, bölümleri çevrimdışı indir ve kaldığın saniyeden yer imleriyle devam et.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onSearchClick,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Icon(Icons.Default.Explore, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Keşfetmeye Başla")
        }
        Spacer(modifier = Modifier.height(10.dp))
        FilledTonalButton(
            onClick = onAddRssClick,
            colors = ButtonDefaults.filledTonalButtonColors(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.RssFeed, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("RSS Bağlantısı Ekle")
        }
    }
}
