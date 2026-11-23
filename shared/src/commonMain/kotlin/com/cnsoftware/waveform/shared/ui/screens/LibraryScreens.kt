@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cnsoftware.waveform.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.EpisodeBookmark
import com.cnsoftware.waveform.shared.model.ListeningStats
import com.cnsoftware.waveform.shared.model.NewEpisodeNotification
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.ui.LibrarySection
import com.cnsoftware.waveform.shared.ui.components.ArtworkWithPlayedBadge
import com.cnsoftware.waveform.shared.ui.components.ConnectedToggleRow
import com.cnsoftware.waveform.shared.ui.components.EpisodeMeta
import com.cnsoftware.waveform.shared.ui.components.EpisodeRowActions
import com.cnsoftware.waveform.shared.ui.components.HomeEpisodeCard
import com.cnsoftware.waveform.shared.ui.components.LocalPlayedIds
import com.cnsoftware.waveform.shared.ui.components.PodcastArtwork
import com.cnsoftware.waveform.shared.ui.components.ShapeBadge
import com.cnsoftware.waveform.shared.ui.components.formatTime
import com.cnsoftware.waveform.shared.ui.formatListeningSeconds

@Composable
internal fun LibraryScreen(
    section: LibrarySection,
    query: String,
    subscriptions: List<Podcast>,
    recentEpisodes: List<Episode>,
    notifications: List<NewEpisodeNotification>,
    favorites: List<Episode>,
    bookmarks: List<EpisodeBookmark>,
    queue: List<Episode>,
    listeningStats: ListeningStats,
    episodePositions: Map<String, Long>,
    downloadedEpisodeIds: Set<String>,
    downloadingEpisodeIds: Set<String>,
    downloadProgress: Map<String, Float>,
    queuedEpisodeIds: Set<String>,
    playedEpisodeIds: Set<String>,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onSelectSection: (LibrarySection) -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onSetPodcastPlayed: (Podcast, Boolean) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onAddRssClick: () -> Unit,
    onSearchClick: () -> Unit,
    onToggleQueue: (Episode) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onPlayBookmark: (EpisodeBookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleDownload: (Episode) -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ListeningStatsBento(
            stats = listeningStats,
            onOpenHistory = onOpenHistory,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        ConnectedToggleRow(
            items = listOf(
                LibrarySection.SUBSCRIPTIONS to "Abonelik",
                LibrarySection.DOWNLOADS to "İndirilen",
                LibrarySection.FAVORITES to "Favori",
                LibrarySection.BOOKMARKS to "Yer İmi"
            ),
            selected = section,
            onSelect = onSelectSection,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            when (section) {
                LibrarySection.SUBSCRIPTIONS -> SubscriptionsScreen(
                    query = query,
                    subscriptions = subscriptions,
                    playedEpisodeIds = playedEpisodeIds,
                    onOpenPodcast = onOpenPodcast,
                    onSetPodcastPlayed = onSetPodcastPlayed,
                    onRemoveSubscription = onRemoveSubscription,
                    onAddRssClick = onAddRssClick,
                    onSearchClick = onSearchClick
                )

                LibrarySection.DOWNLOADS -> DownloadsScreen(
                    subscriptions = subscriptions,
                    recentEpisodes = recentEpisodes,
                    notifications = notifications,
                    favorites = favorites,
                    bookmarks = bookmarks,
                    queue = queue,
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

                LibrarySection.FAVORITES -> FavoritesScreen(
                    favorites = favorites,
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

                LibrarySection.BOOKMARKS -> BookmarksScreen(
                    bookmarks = bookmarks,
                    onPlayBookmark = onPlayBookmark,
                    onDeleteBookmark = onDeleteBookmark
                )
            }
        }
    }
}

@Composable
private fun ListeningStatsBento(
    stats: ListeningStats,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationLabel = formatListeningSeconds(stats.totalListeningSeconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenHistory),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Dinleme Özeti",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = onOpenHistory,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Detaylı Geçmiş")
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMiniCard(
                value = durationLabel,
                label = "Dinleme",
                icon = Icons.Default.Schedule,
                shape = MaterialShapes.Sunny.toShape(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                value = stats.completedEpisodesCount.toString(),
                label = "Tamamlanan",
                icon = Icons.Default.CheckCircle,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                value = "${stats.activeDayKeys.size} gün",
                label = "Aktif",
                icon = Icons.Default.CalendarToday,
                shape = MaterialShapes.Clover4Leaf.toShape(),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatMiniCard(
    value: String,
    label: String,
    icon: ImageVector,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(shape)
                    .background(contentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** İndirilen (ve inmekte olan) bölümler. Bölüm bilgisi bilinen kaynaklardan toplanır. */
@Composable
internal fun DownloadsScreen(
    subscriptions: List<Podcast>,
    recentEpisodes: List<Episode>,
    notifications: List<NewEpisodeNotification>,
    favorites: List<Episode>,
    bookmarks: List<EpisodeBookmark>,
    queue: List<Episode>,
    episodePositions: Map<String, Long>,
    downloadedEpisodeIds: Set<String>,
    downloadingEpisodeIds: Set<String>,
    downloadProgress: Map<String, Float>,
    queuedEpisodeIds: Set<String>,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onToggleQueue: (Episode) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleDownload: (Episode) -> Unit
) {
    val episodes = remember(
        subscriptions, recentEpisodes, notifications, favorites, bookmarks, queue,
        downloadedEpisodeIds, downloadingEpisodeIds
    ) {
        val wanted = downloadedEpisodeIds + downloadingEpisodeIds
        if (wanted.isEmpty()) {
            emptyList()
        } else {
            (recentEpisodes + favorites + bookmarks.map { it.episode } + queue +
                notifications.map { it.episode } + subscriptions.flatMap { it.episodes })
                .distinctBy { it.id }
                .filter { it.id in wanted }
        }
    }

    if (episodes.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShapeBadge(
                icon = Icons.Default.DownloadDone,
                shape = MaterialShapes.Cookie9Sided.toShape()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Henüz indirilen bölüm yok",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bir bölümün ⋮ menüsünden \"Çevrimdışı indir\" diyerek internetsiz dinleyebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "${episodes.size} bölüm",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        items(episodes, key = { it.id }) { episode ->
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

/** Favori bölümlerin tam listesi. */
@Composable
internal fun FavoritesScreen(
    favorites: List<Episode>,
    episodePositions: Map<String, Long>,
    downloadedEpisodeIds: Set<String>,
    downloadingEpisodeIds: Set<String>,
    downloadProgress: Map<String, Float>,
    queuedEpisodeIds: Set<String>,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onToggleQueue: (Episode) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleDownload: (Episode) -> Unit
) {
    if (favorites.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShapeBadge(
                icon = Icons.Default.Favorite,
                shape = MaterialShapes.Heart.toShape(),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Henüz favori bölümün yok",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bir bölümün ⋮ menüsünden ya da oynatıcıdaki kalp simgesinden favorilere ekleyebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "${favorites.size} favori bölüm",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        items(favorites, key = { it.id }) { episode ->
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

@Composable
internal fun BookmarksScreen(
    bookmarks: List<EpisodeBookmark>,
    onPlayBookmark: (EpisodeBookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    if (bookmarks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShapeBadge(
                icon = Icons.Default.Bookmark,
                shape = MaterialShapes.Gem.toShape()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Henüz yer imin yok",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bölüm dinlerken oynatıcı araç çubuğundaki yer imi simgesine dokunarak önemli anları not edebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "${bookmarks.size} kayıtlı an",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        items(bookmarks, key = { it.id }) { bookmark ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayBookmark(bookmark) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PodcastArtwork(
                        imageUrl = bookmark.episode.imageUrl,
                        size = 52.dp,
                        cornerRadius = 14.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = formatTime(bookmark.positionMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = bookmark.episode.podcastTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bookmark.episode.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (bookmark.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = bookmark.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { onDeleteBookmark(bookmark.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Yer imini sil")
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotificationsScreen(
    notifications: List<NewEpisodeNotification>,
    refreshMessage: String?,
    hasSubscriptions: Boolean,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    episodePositions: Map<String, Long>,
    downloadedEpisodeIds: Set<String>,
    downloadingEpisodeIds: Set<String>,
    downloadProgress: Map<String, Float>,
    queuedEpisodeIds: Set<String>,
    onToggleQueue: (Episode) -> Unit,
    onToggleDownload: (Episode) -> Unit,
    onMarkAllRead: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearNotifications: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (refreshMessage != null || notifications.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (refreshMessage != null) {
                        Text(
                            text = refreshMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (notifications.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (notifications.any { !it.isRead }) {
                                TextButton(
                                    onClick = onMarkAllRead,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Tümünü okundu say")
                                }
                            }
                            TextButton(
                                onClick = { showClearDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Temizle")
                            }
                        }
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShapeBadge(
                    icon = Icons.Default.Notifications,
                    shape = MaterialShapes.SoftBurst.toShape()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Henüz yeni bölüm bildirimi yok",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasSubscriptions) {
                        "Yeni bölümleri kontrol etmek için ekranı aşağı çekebilirsin."
                    } else {
                        "Yeni bölüm bildirimleri almak için önce bir podcast'e abone ol."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications, key = { it.episode.id }) { item ->
                val episode = item.episode
                val isCurrent = episode.id == currentlyPlayingId
                val isDownloaded = episode.id in downloadedEpisodeIds
                val isPlayed = episode.id in LocalPlayedIds.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (isCurrent) onTogglePlayPause() else onPlayEpisode(episode) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isRead) {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkWithPlayedBadge(isPlayed = isPlayed) {
                            PodcastArtwork(
                                imageUrl = episode.imageUrl,
                                size = 50.dp,
                                cornerRadius = 14.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!item.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                                Text(
                                    text = episode.podcastTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDismissNotification(episode.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Bildirimi kaldır",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            EpisodeMeta(
                                episode = episode,
                                savedMs = episodePositions[episode.id] ?: 0L,
                                isPlayed = isPlayed,
                                isDownloaded = isDownloaded
                            )
                        }
                        EpisodeRowActions(
                            episode = episode,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            isQueued = episode.id in queuedEpisodeIds,
                            isDownloaded = isDownloaded,
                            isDownloading = episode.id in downloadingEpisodeIds,
                            downloadProgress = downloadProgress[episode.id],
                            isPlayed = null,
                            onTogglePlayed = null,
                            onToggleQueue = { onToggleQueue(episode) },
                            onToggleDownload = { onToggleDownload(episode) },
                            onPlay = { if (isCurrent) onTogglePlayPause() else onPlayEpisode(episode) }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Bildirimleri temizle") },
            text = { Text("Listedeki tüm yeni bölüm bildirimleri kaldırılacak.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearNotifications()
                    }
                ) {
                    Text("Temizle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}
