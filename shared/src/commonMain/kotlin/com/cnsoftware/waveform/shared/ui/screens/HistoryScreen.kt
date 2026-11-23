@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cnsoftware.waveform.shared.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.ListeningHistoryEntry
import com.cnsoftware.waveform.shared.model.ListeningStats
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.ui.DailyListeningBar
import com.cnsoftware.waveform.shared.ui.HistoryTimeBucket
import com.cnsoftware.waveform.shared.ui.calculateListeningStreakDays
import com.cnsoftware.waveform.shared.ui.components.ArtworkWithPlayedBadge
import com.cnsoftware.waveform.shared.ui.components.ConnectedToggleRow
import com.cnsoftware.waveform.shared.ui.components.PodcastArtwork
import com.cnsoftware.waveform.shared.ui.components.ShapeBadge
import com.cnsoftware.waveform.shared.ui.components.formatTime
import com.cnsoftware.waveform.shared.ui.components.morphingPlayShape
import com.cnsoftware.waveform.shared.ui.formatDurationShort
import com.cnsoftware.waveform.shared.ui.formatHistoryTimestamp
import com.cnsoftware.waveform.shared.ui.formatListeningSeconds
import com.cnsoftware.waveform.shared.ui.historyTimeBucket
import com.cnsoftware.waveform.shared.ui.lastSevenDaysActivity
import com.cnsoftware.waveform.shared.ui.localUtcOffsetMillis
import com.cnsoftware.waveform.shared.ui.nowMillis
import com.cnsoftware.waveform.shared.ui.parseDurationMs

private enum class HistoryFilter {
    ALL,
    IN_PROGRESS,
    COMPLETED
}

@Composable
internal fun HistoryScreen(
    history: List<ListeningHistoryEntry>,
    listeningStats: ListeningStats,
    subscriptions: List<Podcast>,
    episodePositions: Map<String, Long>,
    playedEpisodeIds: Set<String>,
    queuedEpisodeIds: Set<String>,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onPlayEpisode: (Episode) -> Unit,
    onReplayFromStart: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleQueue: (Episode) -> Unit,
    onTogglePlayed: (Episode) -> Unit,
    onRemoveHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onExploreClick: () -> Unit
) {
    if (history.isEmpty() && listeningStats.totalListeningSeconds == 0L) {
        HistoryEmptyState(onExploreClick = onExploreClick)
        return
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    var showClearDialog by remember { mutableStateOf(false) }

    val nowMs = remember(history, listeningStats) { nowMillis() }
    val utcOffsetMs = remember(nowMs) { localUtcOffsetMillis(nowMs) }
    val streakDays = remember(listeningStats.activeDayKeys, nowMs, utcOffsetMs) {
        calculateListeningStreakDays(listeningStats.activeDayKeys, nowMs, utcOffsetMs)
    }
    val weeklyBars = remember(listeningStats.dailyListeningSeconds, nowMs, utcOffsetMs) {
        lastSevenDaysActivity(listeningStats.dailyListeningSeconds, nowMs, utcOffsetMs)
    }
    val sessionCount = remember(history) { history.sumOf { it.playCount } }

    val trimmedQuery = searchQuery.trim()
    val filteredHistory = remember(history, playedEpisodeIds, trimmedQuery, selectedFilter) {
        history.filter { entry ->
            val completed = entry.completed || entry.episode.id in playedEpisodeIds
            val matchesFilter = when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.IN_PROGRESS -> !completed
                HistoryFilter.COMPLETED -> completed
            }
            val matchesQuery = trimmedQuery.isEmpty() ||
                entry.episode.title.contains(trimmedQuery, ignoreCase = true) ||
                entry.episode.podcastTitle.contains(trimmedQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    val groupedHistory = remember(filteredHistory, nowMs, utcOffsetMs) {
        val byBucket = filteredHistory.groupBy { entry ->
            historyTimeBucket(entry.lastPlayedAtMs, nowMs, utcOffsetMs)
        }
        listOf(
            HistoryTimeBucket.TODAY,
            HistoryTimeBucket.YESTERDAY,
            HistoryTimeBucket.THIS_WEEK,
            HistoryTimeBucket.OLDER
        ).mapNotNull { bucket ->
            val entries = byBucket[bucket].orEmpty()
            if (entries.isEmpty()) null else bucket to entries
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "summary") {
            HistorySummaryCard(
                stats = listeningStats,
                sessionCount = sessionCount,
                streakDays = streakDays,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "weekly") {
            WeeklyActivityCard(
                bars = weeklyBars,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        val topPodcasts = listeningStats.podcastListeningSeconds.entries
            .filter { it.value > 0L }
            .sortedByDescending { it.value }
            .take(4)
        if (topPodcasts.isNotEmpty()) {
            item(key = "top_podcasts") {
                TopPodcastsCard(
                    topPodcasts = topPodcasts,
                    subscriptions = subscriptions,
                    history = history,
                    onOpenPodcast = onOpenPodcast,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item(key = "timeline_controls") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dinleme Zaman Tüneli",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = filteredHistory.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { showClearDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Temizle")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Geçmişte bölüm veya podcast ara") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                ConnectedToggleRow(
                    items = listOf(
                        HistoryFilter.ALL to "Tümü",
                        HistoryFilter.IN_PROGRESS to "Devam Eden",
                        HistoryFilter.COMPLETED to "Tamamlanan"
                    ),
                    selected = selectedFilter,
                    onSelect = { selectedFilter = it }
                )
            }
        }

        if (groupedHistory.isEmpty()) {
            item(key = "empty_filtered") {
                Text(
                    text = if (history.isEmpty()) {
                        "Bölüm oynattığında detaylı dinleme kayıtları burada kronolojik olarak birikir."
                    } else {
                        "Bu filtre veya arama için geçmiş kaydı bulunamadı."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        } else {
            groupedHistory.forEach { (bucket, entries) ->
                item(key = "bucket-${bucket.name}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bucketTitle(bucket),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = entries.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                items(entries, key = { "h-${it.episode.id}" }) { entry ->
                    val episode = entry.episode
                    val isCurrent = episode.id == currentlyPlayingId
                    val effectiveCompleted = entry.completed || episode.id in playedEpisodeIds
                    val effectivePos = episodePositions[episode.id] ?: entry.lastPositionMs

                    HistoryEntryCard(
                        entry = entry,
                        effectivePositionMs = effectivePos,
                        effectiveCompleted = effectiveCompleted,
                        isQueued = episode.id in queuedEpisodeIds,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        nowMs = nowMs,
                        utcOffsetMs = utcOffsetMs,
                        onPlayOrToggle = {
                            if (isCurrent) onTogglePlayPause() else onPlayEpisode(episode)
                        },
                        onReplayFromStart = { onReplayFromStart(episode) },
                        onToggleQueue = { onToggleQueue(episode) },
                        onTogglePlayed = { onTogglePlayed(episode) },
                        onRemove = { onRemoveHistoryEntry(episode.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                showClearDialog = false
                onClearHistory()
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

private fun bucketTitle(bucket: HistoryTimeBucket): String = when (bucket) {
    HistoryTimeBucket.TODAY -> "Bugün"
    HistoryTimeBucket.YESTERDAY -> "Dün"
    HistoryTimeBucket.THIS_WEEK -> "Bu Hafta"
    HistoryTimeBucket.OLDER -> "Daha Önce"
}

@Composable
private fun HistoryEmptyState(onExploreClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShapeBadge(
            icon = Icons.Default.History,
            shape = MaterialShapes.Cookie12Sided.toShape(),
            size = 112.dp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Dinleme geçmişin henüz boş",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dinlediğin bölümler, toplam süren, günlük aktiviten ve kaldığın saniyeler burada birikir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onExploreClick,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.Explore, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Keşfetmeye Başla")
        }
    }
}

@Composable
private fun HistorySummaryCard(
    stats: ListeningStats,
    sessionCount: Int,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colorScheme.primaryContainer,
                            colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            colorScheme.surfaceContainerHigh
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TOPLAM DİNLEME",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatListeningSeconds(stats.totalListeningSeconds),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (streakDays > 0) colorScheme.tertiaryContainer else colorScheme.surface.copy(alpha = 0.72f),
                        contentColor = if (streakDays > 0) colorScheme.onTertiaryContainer else colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (streakDays > 0) "$streakDays gün seri" else "Yeni seri başlat",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryMetricPill(
                        value = stats.completedEpisodesCount.toString(),
                        label = "Tamamlanan",
                        modifier = Modifier.weight(1f)
                    )
                    HistoryMetricPill(
                        value = stats.activeDayKeys.size.toString(),
                        label = "Aktif Gün",
                        modifier = Modifier.weight(1f)
                    )
                    HistoryMetricPill(
                        value = sessionCount.toString(),
                        label = "Oturum",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryMetricPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyActivityCard(
    bars: List<DailyListeningBar>,
    modifier: Modifier = Modifier
) {
    val maxSeconds = bars.maxOfOrNull { it.seconds }?.coerceAtLeast(1L) ?: 1L
    val totalWeekSeconds = bars.sumOf { it.seconds }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Son 7 Gün",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = formatListeningSeconds(totalWeekSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val ratio = if (bar.seconds <= 0L) 0f else (bar.seconds.toFloat() / maxSeconds.toFloat())
                    val barHeight = if (bar.seconds <= 0L) 6.dp else (10f + 66f * ratio).dp
                    val barColor = when {
                        bar.seconds <= 0L -> MaterialTheme.colorScheme.surfaceContainerHighest
                        bar.isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.height(16.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (bar.seconds > 0L) {
                                Text(
                                    text = formatCompactBarDuration(bar.seconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (bar.isToday) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .height(76.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(barColor)
                            )
                        }
                        Text(
                            text = bar.shortDayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (bar.isToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatCompactBarDuration(seconds: Long): String = when {
    seconds < 60L -> "${seconds}s"
    seconds < 3600L -> "${seconds / 60L}d"
    else -> "${seconds / 3600L}sa"
}

@Composable
private fun TopPodcastsCard(
    topPodcasts: List<Map.Entry<String, Long>>,
    subscriptions: List<Podcast>,
    history: List<ListeningHistoryEntry>,
    onOpenPodcast: (Podcast) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxSeconds = topPodcasts.firstOrNull()?.value?.coerceAtLeast(1L) ?: 1L
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "En Çok Dinlenen Podcastler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            topPodcasts.forEachIndexed { index, (podcastTitle, seconds) ->
                val matchedSub = subscriptions.firstOrNull { it.title == podcastTitle }
                val artworkUrl = matchedSub?.imageUrl
                    ?: history.firstOrNull { it.episode.podcastTitle == podcastTitle }?.episode?.imageUrl
                val fraction = (seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.08f, 1f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (matchedSub != null) Modifier.clickable { onOpenPodcast(matchedSub) }
                            else Modifier
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (index == 0) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (index == 0) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    PodcastArtwork(
                        imageUrl = artworkUrl,
                        size = 40.dp,
                        cornerRadius = 12.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = podcastTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatListeningSeconds(seconds),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (index == 0) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: ListeningHistoryEntry,
    effectivePositionMs: Long,
    effectiveCompleted: Boolean,
    isQueued: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    nowMs: Long,
    utcOffsetMs: Long,
    onPlayOrToggle: () -> Unit,
    onReplayFromStart: () -> Unit,
    onToggleQueue: () -> Unit,
    onTogglePlayed: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val episode = entry.episode
    val effectiveDurationMs = entry.durationMs.takeIf { it > 0L } ?: (parseDurationMs(episode.durationText) ?: 0L)
    val progress = if (!effectiveCompleted && effectiveDurationMs > 0L && effectivePositionMs > 0L) {
        (effectivePositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }
    val timestampLabel = remember(entry.lastPlayedAtMs, entry.playCount, nowMs, utcOffsetMs) {
        buildString {
            append(formatHistoryTimestamp(entry.lastPlayedAtMs, nowMs, utcOffsetMs))
            if (entry.playCount > 1) append(" • ${entry.playCount} kez")
        }
    }
    val metricsLabel = remember(
        effectiveCompleted,
        effectivePositionMs,
        effectiveDurationMs,
        episode.durationText,
        entry.totalListenedSeconds
    ) {
        buildList {
            when {
                effectiveCompleted -> add("Tamamlandı")
                effectivePositionMs > 0L -> add("Kaldığın yer ${formatTime(effectivePositionMs)}")
            }
            if (effectiveDurationMs > 0L) {
                add(formatDurationShort(effectiveDurationMs))
            } else if (episode.durationText.isNotBlank()) {
                add(episode.durationText.trim())
            }
            if (entry.totalListenedSeconds >= 30L) {
                add("${formatListeningSeconds(entry.totalListenedSeconds)} dinlendi")
            }
        }.joinToString(" • ")
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayOrToggle),
        shape = RoundedCornerShape(24.dp),
        border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkWithPlayedBadge(isPlayed = effectiveCompleted) {
                    PodcastArtwork(
                        imageUrl = episode.imageUrl,
                        size = 56.dp,
                        cornerRadius = 16.dp,
                        shape = if (isPlaying) {
                            MaterialShapes.Cookie12Sided.toShape()
                        } else {
                            RoundedCornerShape(16.dp)
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = timestampLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = episode.podcastTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (metricsLabel.isNotBlank()) {
                        Text(
                            text = metricsLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledIconButton(
                    onClick = onPlayOrToggle,
                    shape = morphingPlayShape(isPlaying),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (isCurrent) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isPlaying -> Icons.Default.Pause
                            effectiveCompleted -> Icons.Default.Replay
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = "Oynat / Duraklat"
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Geçmiş seçenekleri")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Baştan dinle") },
                            leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onReplayFromStart()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isQueued) "Sıradan çıkar" else "Sıraya ekle") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isQueued) {
                                        Icons.AutoMirrored.Filled.PlaylistAddCheck
                                    } else {
                                        Icons.AutoMirrored.Filled.PlaylistAdd
                                    },
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleQueue()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (effectiveCompleted) "Dinlenmedi işaretle" else "Dinlendi işaretle") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (effectiveCompleted) {
                                        Icons.Default.RadioButtonUnchecked
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePlayed()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Geçmişten kaldır") },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRemove()
                            }
                        )
                    }
                }
            }

            if (progress != null) {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    amplitude = { if (isPlaying) 1f else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dinleme geçmişini temizle") },
        text = {
            Text("Tüm geçmiş kayıtları, dinleme süresi istatistiklerin ve kaldığın konumlar silinecek.")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Temizle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
