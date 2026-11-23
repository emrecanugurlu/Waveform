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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.ui.components.ConnectedToggleRow
import com.cnsoftware.waveform.shared.ui.components.EpisodeMeta
import com.cnsoftware.waveform.shared.ui.components.EpisodeRowActions
import com.cnsoftware.waveform.shared.ui.components.ExpandableText
import com.cnsoftware.waveform.shared.ui.components.LocalFavorites
import com.cnsoftware.waveform.shared.ui.components.PodcastArtwork

private enum class DetailEpisodeFilter {
    ALL,
    UNPLAYED,
    DOWNLOADED,
    FAVORITES
}

@Composable
internal fun PodcastDetailScreen(
    podcast: Podcast,
    episodePositions: Map<String, Long>,
    downloadedEpisodeIds: Set<String>,
    downloadingEpisodeIds: Set<String>,
    downloadProgress: Map<String, Float>,
    queuedEpisodeIds: Set<String>,
    playedEpisodeIds: Set<String>,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onToggleQueue: (Episode) -> Unit,
    onTogglePlayed: (Episode) -> Unit,
    onSetPodcastPlayed: (Podcast, Boolean) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleDownload: (Episode) -> Unit
) {
    var episodeQuery by rememberSaveable(podcast.feedUrl) { mutableStateOf("") }
    var oldestFirst by rememberSaveable(podcast.feedUrl) { mutableStateOf(false) }
    var filter by rememberSaveable(podcast.feedUrl) { mutableStateOf(DetailEpisodeFilter.ALL) }
    var actionsMenuOpen by remember { mutableStateOf(false) }
    val favoritesState = LocalFavorites.current

    val playedCount = remember(podcast, playedEpisodeIds) {
        podcast.episodes.count { it.id in playedEpisodeIds }
    }
    val allPlayed = podcast.episodes.isNotEmpty() && playedCount == podcast.episodes.size
    val completion = if (podcast.episodes.isNotEmpty()) {
        playedCount.toFloat() / podcast.episodes.size.toFloat()
    } else {
        0f
    }

    val visibleEpisodes = remember(
        podcast, playedEpisodeIds, downloadedEpisodeIds, favoritesState.ids,
        episodeQuery, oldestFirst, filter
    ) {
        val q = episodeQuery.trim()
        val base = if (oldestFirst) podcast.episodes.asReversed() else podcast.episodes
        base.filter { ep ->
            val matchesQuery = q.isEmpty() || ep.title.contains(q, ignoreCase = true)
            val matchesFilter = when (filter) {
                DetailEpisodeFilter.ALL -> true
                DetailEpisodeFilter.UNPLAYED -> ep.id !in playedEpisodeIds
                DetailEpisodeFilter.DOWNLOADED -> ep.id in downloadedEpisodeIds
                DetailEpisodeFilter.FAVORITES -> ep.id in favoritesState.ids
            }
            matchesQuery && matchesFilter
        }
    }

    val nextUnplayedOrLatest = remember(podcast, playedEpisodeIds) {
        podcast.episodes.firstOrNull { it.id !in playedEpisodeIds } ?: podcast.episodes.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PodcastArtwork(
                                imageUrl = podcast.imageUrl,
                                size = 82.dp,
                                cornerRadius = 20.dp,
                                shape = MaterialShapes.Cookie9Sided.toShape()
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = podcast.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (podcast.author.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = podcast.author,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$playedCount / ${podcast.episodes.size} bölüm dinlendi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (podcast.description.isNotBlank()) {
                            ExpandableText(
                                text = podcast.description,
                                style = MaterialTheme.typography.bodyMedium,
                                collapsedLines = 3
                            )
                        }

                        if (podcast.episodes.isNotEmpty()) {
                            LinearWavyProgressIndicator(
                                progress = { completion },
                                amplitude = { 0f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                SplitButtonLayout(
                                    leadingButton = {
                                        SplitButtonDefaults.LeadingButton(
                                            enabled = nextUnplayedOrLatest != null,
                                            onClick = { nextUnplayedOrLatest?.let(onPlayEpisode) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sonraki Bölümü Çal")
                                        }
                                    },
                                    trailingButton = {
                                        SplitButtonDefaults.TrailingButton(
                                            onClick = { actionsMenuOpen = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Podcast işlemleri",
                                                modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize)
                                            )
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = actionsMenuOpen,
                                    onDismissRequest = { actionsMenuOpen = false }
                                ) {
                                    if (podcast.episodes.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (allPlayed) "Hiçbirini dinlendi sayma" else "Tümünü dinlendi say"
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            },
                                            onClick = {
                                                actionsMenuOpen = false
                                                onSetPodcastPlayed(podcast, !allPlayed)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Abonelikten çık") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                        },
                                        onClick = {
                                            actionsMenuOpen = false
                                            onRemoveSubscription(podcast.feedUrl)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = episodeQuery,
                        onValueChange = { episodeQuery = it },
                        placeholder = { Text("Bölümlerde ara") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (episodeQuery.isNotEmpty()) {
                                IconButton(onClick = { episodeQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Temizle")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = oldestFirst,
                        onClick = { oldestFirst = !oldestFirst },
                        leadingIcon = {
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text(if (oldestFirst) "Eski" else "Yeni") }
                    )
                }

                ConnectedToggleRow(
                    items = listOf(
                        DetailEpisodeFilter.ALL to "Tümü",
                        DetailEpisodeFilter.UNPLAYED to "Yeni",
                        DetailEpisodeFilter.DOWNLOADED to "İndirilen",
                        DetailEpisodeFilter.FAVORITES to "Favori"
                    ),
                    selected = filter,
                    onSelect = { filter = it }
                )
            }
        }

        if (visibleEpisodes.isEmpty()) {
            item {
                Text(
                    text = "Eşleşen bölüm bulunamadı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(visibleEpisodes, key = { it.id }) { episode ->
                val isCurrent = episode.id == currentlyPlayingId
                val isPlayed = episode.id in playedEpisodeIds
                val isQueued = episode.id in queuedEpisodeIds
                val isDownloaded = episode.id in downloadedEpisodeIds
                val isDownloading = episode.id in downloadingEpisodeIds
                val savedMs = episodePositions[episode.id] ?: 0L
                val cardShape = RoundedCornerShape(24.dp)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (isCurrent) onTogglePlayPause() else onPlayEpisode(episode) },
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            isPlayed -> MaterialTheme.colorScheme.surfaceContainerLowest
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlayed) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Dinlendi",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(end = 10.dp).size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isPlayed) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            EpisodeMeta(
                                episode = episode,
                                savedMs = savedMs,
                                isPlayed = isPlayed,
                                isDownloaded = isDownloaded
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        EpisodeRowActions(
                            episode = episode,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            isQueued = isQueued,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            downloadProgress = downloadProgress[episode.id],
                            isPlayed = isPlayed,
                            onTogglePlayed = { onTogglePlayed(episode) },
                            onToggleQueue = { onToggleQueue(episode) },
                            onToggleDownload = { onToggleDownload(episode) },
                            onPlay = { if (isCurrent) onTogglePlayPause() else onPlayEpisode(episode) }
                        )
                    }
                }
            }
        }
    }
}
