@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cnsoftware.waveform.shared.ui.player

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.EpisodeBookmark
import com.cnsoftware.waveform.shared.model.PlayerState
import com.cnsoftware.waveform.shared.ui.PlatformBackHandler
import com.cnsoftware.waveform.shared.ui.components.ConnectedToggleRow
import com.cnsoftware.waveform.shared.ui.components.LiveWaveformBars
import com.cnsoftware.waveform.shared.ui.components.LocalFavorites
import com.cnsoftware.waveform.shared.ui.components.PodcastArtwork
import com.cnsoftware.waveform.shared.ui.components.SettingsActionRow
import com.cnsoftware.waveform.shared.ui.components.SettingsSectionTitle
import com.cnsoftware.waveform.shared.ui.components.SettingsSwitchRow
import com.cnsoftware.waveform.shared.ui.components.formatTime
import com.cnsoftware.waveform.shared.ui.components.morphingPlayShape
import com.cnsoftware.waveform.shared.ui.parseEpisodeDescription

@Composable
internal fun MiniPlayerBar(
    playerState: PlayerState,
    skipForwardSeconds: Int,
    onTogglePlayPause: () -> Unit,
    onQuickForward: () -> Unit,
    onClick: () -> Unit
) {
    val episode = playerState.currentEpisode ?: return
    val progress = if (playerState.durationMs > 0L) {
        (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 4.dp,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PodcastArtwork(
                    imageUrl = episode.imageUrl,
                    size = 46.dp,
                    cornerRadius = 12.dp,
                    shape = MaterialShapes.Cookie9Sided.toShape()
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (playerState.errorMessage == null) {
                            LiveWaveformBars(
                                isPlaying = playerState.isPlaying,
                                barCount = 4,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(width = 16.dp, height = 11.dp)
                            )
                        }
                        Text(
                            text = playerState.errorMessage ?: episode.podcastTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (playerState.errorMessage != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onQuickForward) {
                    Icon(
                        imageVector = Icons.Default.Forward30,
                        contentDescription = "+$skipForwardSeconds sn"
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    shape = morphingPlayShape(playerState.isPlaying),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Oynat / Duraklat"
                    )
                }
            }
            if (playerState.isBuffering) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                )
            } else {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    amplitude = { if (playerState.isPlaying) 1f else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}

/**
 * Oynatıcıdaki çalma sırası alt sayfası: kayan araç çubuğu veya seçeneklerden açılır.
 */
@Composable
private fun QueueSheet(
    queue: List<Episode>,
    onPlay: (Episode) -> Unit,
    onRemove: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = if (queue.isNotEmpty()) 8.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sıradaki (${queue.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (queue.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Temizle") }
                }
            }

            if (queue.isEmpty()) {
                Text(
                    text = "Sıra boş. Bölüm satırlarındaki sıra simgesiyle bölüm ekleyebilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    itemsIndexed(queue, key = { _, episode -> episode.id }) { index, episode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlay(episode)
                                    onDismiss()
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PodcastArtwork(imageUrl = episode.imageUrl, size = 48.dp, cornerRadius = 12.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = episode.title,
                                    style = MaterialTheme.typography.titleSmall,
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
                            }
                            IconButton(onClick = { onMoveUp(episode.id) }, enabled = index > 0) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Yukarı taşı")
                            }
                            IconButton(onClick = { onMoveDown(episode.id) }, enabled = index < queue.lastIndex) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Aşağı taşı")
                            }
                            IconButton(onClick = { onRemove(episode.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Sıradan çıkar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerButton(
    remainingMs: Long?,
    atEndOfEpisode: Boolean,
    onStartTimer: (Int) -> Unit,
    onSleepAtEnd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isActive = remainingMs != null || atEndOfEpisode
    val label = when {
        remainingMs != null -> "Uyku: ${formatTime(remainingMs)}"
        atEndOfEpisode -> "Uyku: bölüm sonunda"
        else -> "Uyku zamanlayıcı"
    }

    Box(modifier = modifier) {
        FilledTonalButton(
            onClick = { menuExpanded = true },
            colors = if (isActive) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            } else {
                ButtonDefaults.filledTonalButtonColors()
            }
        ) {
            Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label)
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
                DropdownMenuItem(
                    text = { Text("$minutes dakika") },
                    onClick = {
                        menuExpanded = false
                        onStartTimer(minutes)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Bölüm sonunda") },
                onClick = {
                    menuExpanded = false
                    onSleepAtEnd()
                }
            )
            if (isActive) {
                DropdownMenuItem(
                    text = { Text("Zamanlayıcıyı kapat") },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onCancel()
                    }
                )
            }
        }
    }
}

/**
 * Bölüm açıklaması: başlık + okunaklı paragraflar; açıklamada zaman damgaları varsa dokunulabilir
 * "Bölüm içeriği" listesi (dokununca o ana atlar).
 */
@Composable
private fun EpisodeDescriptionSheet(
    episode: Episode,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val parsed = remember(episode.id, episode.description) { parseEpisodeDescription(episode.description) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PodcastArtwork(imageUrl = episode.imageUrl, size = 56.dp, cornerRadius = 14.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.podcastTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (parsed.paragraphs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Hakkında",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    parsed.paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp
                        )
                    }
                }
            }

            if (parsed.chapters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Bölüm içeriği",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Dokunarak o ana git",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        parsed.chapters.forEach { chapter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSeekTo(chapter.timeMs)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = formatTime(chapter.timeMs),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = chapter.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
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
private fun EpisodeBookmarksSheet(
    episode: Episode,
    currentPositionMs: Long,
    bookmarks: List<EpisodeBookmark>,
    onAddBookmark: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Yer İmleri (${formatTime(currentPositionMs)})",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("İsteğe bağlı kısa not (ör. Harika alıntı)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onAddBookmark(note)
                    note = ""
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bu Anı Kaydet (${formatTime(currentPositionMs)})")
            }

            if (bookmarks.isEmpty()) {
                Text(
                    text = "Bu bölüm için henüz yer imi eklenmemiş.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSeekTo(bm.positionMs)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = formatTime(bm.positionMs),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = bm.note.ifBlank { "Kayıtlı an" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerOptionsSheet(
    playbackSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    sleepTimerRemainingMs: Long?,
    sleepAtEndOfEpisode: Boolean,
    onStartSleepTimer: (Int) -> Unit,
    onSleepAtEndOfEpisode: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    autoPlayNext: Boolean,
    onSetAutoPlayNext: (Boolean) -> Unit,
    queueSize: Int,
    onOpenQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    val speedRow1 = listOf(0.75f to "0.75x", 1.0f to "1x", 1.25f to "1.25x", 1.5f to "1.5x")
    val speedRow2 = listOf(1.75f to "1.75x", 2.0f to "2x", 2.5f to "2.5x")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Oynatma seçenekleri",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            SettingsSectionTitle("Oynatma hızı")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConnectedToggleRow(
                    items = speedRow1,
                    selected = playbackSpeed,
                    onSelect = onSelectSpeed
                )
                ConnectedToggleRow(
                    items = speedRow2,
                    selected = playbackSpeed,
                    onSelect = onSelectSpeed
                )
            }

            SettingsSectionTitle("Uyku zamanlayıcı")
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SleepTimerButton(
                    remainingMs = sleepTimerRemainingMs,
                    atEndOfEpisode = sleepAtEndOfEpisode,
                    onStartTimer = onStartSleepTimer,
                    onSleepAtEnd = onSleepAtEndOfEpisode,
                    onCancel = onCancelSleepTimer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            SettingsSwitchRow(
                title = "Otomatik sonraki bölüm",
                description = "Bölüm bitince sıradaki bölüm kendiliğinden başlar.",
                checked = autoPlayNext,
                onCheckedChange = onSetAutoPlayNext
            )
            SettingsActionRow(
                title = "Çalma sırası ($queueSize)",
                description = "Sıradaki bölümleri gör ve düzenle.",
                enabled = true,
                onClick = onOpenQueue
            )
        }
    }
}

@Composable
internal fun FullPlayerScreen(
    playerState: PlayerState,
    skipBackSeconds: Int,
    skipForwardSeconds: Int,
    bookmarks: List<EpisodeBookmark>,
    onAddBookmark: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    sleepTimerRemainingMs: Long?,
    sleepAtEndOfEpisode: Boolean,
    onStartSleepTimer: (Int) -> Unit,
    onSleepAtEndOfEpisode: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    autoPlayNext: Boolean,
    onSetAutoPlayNext: (Boolean) -> Unit,
    queue: List<Episode>,
    onPlayQueued: (Episode) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onMoveUpInQueue: (String) -> Unit,
    onMoveDownInQueue: (String) -> Unit,
    onClearQueue: () -> Unit
) {
    val episode = playerState.currentEpisode ?: return
    var showOptions by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showDescription by remember(episode.id) { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }

    PlatformBackHandler(enabled = true) { onClose() }

    val duration = playerState.durationMs.coerceAtLeast(0L)
    val position = playerState.positionMs.coerceIn(0L, duration.coerceAtLeast(1L))
    var dragPositionMs by remember(episode.id) { mutableStateOf<Long?>(null) }
    val displayedPosition = (dragPositionMs ?: position).coerceIn(0L, duration.coerceAtLeast(1L))

    val parsedDescription = remember(episode.id, episode.description) {
        parseEpisodeDescription(episode.description)
    }
    val episodeBookmarks = remember(bookmarks, episode.id) {
        bookmarks.filter { it.episode.id == episode.id }
    }
    val activeChapter = remember(parsedDescription.chapters, displayedPosition) {
        parsedDescription.chapters.lastOrNull { it.timeMs <= displayedPosition }
    }

    val artworkScale by animateFloatAsState(
        targetValue = if (playerState.isPlaying) 1.0f else 0.92f,
        label = "artworkScale"
    )
    val artworkShape = if (playerState.isPlaying) {
        MaterialShapes.Cookie12Sided.toShape()
    } else {
        RoundedCornerShape(32.dp)
    }

    val activeOptions = buildList {
        if (playerState.playbackSpeed != 1.0f) add("${playerState.playbackSpeed}x")
        when {
            sleepTimerRemainingMs != null -> add("Uyku ${formatTime(sleepTimerRemainingMs)}")
            sleepAtEndOfEpisode -> add("Uyku: bölüm sonu")
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiveWaveformBars(
                                    isPlaying = playerState.isPlaying,
                                    barCount = 4,
                                    modifier = Modifier.size(width = 18.dp, height = 13.dp)
                                )
                                Text(
                                    text = episode.podcastTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Küçült"
                                )
                            }
                        },
                        actions = {
                            val favorites = LocalFavorites.current
                            val isFavorite = episode.id in favorites.ids
                            IconButton(onClick = { favorites.toggle(episode) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { showOptions = true }) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Oynatma seçenekleri"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val bodyHeight = maxHeight
                    val artworkSize = (bodyHeight - 440.dp)
                        .coerceIn(132.dp, 288.dp)
                        .coerceAtMost(maxWidth - 64.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = bodyHeight)
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .graphicsLayer {
                                    scaleX = artworkScale
                                    scaleY = artworkScale
                                }
                        ) {
                            PodcastArtwork(
                                imageUrl = episode.imageUrl,
                                size = artworkSize,
                                cornerRadius = 32.dp,
                                shape = artworkShape
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (activeChapter != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                AssistChip(
                                    onClick = { showDescription = true },
                                    label = {
                                        Text(
                                            text = activeChapter.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            } else if (activeOptions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                AssistChip(
                                    onClick = { showOptions = true },
                                    label = { Text(activeOptions.joinToString(" • ")) }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Slider(
                                value = if (duration > 0L) displayedPosition.toFloat() else 0f,
                                onValueChange = { dragPositionMs = it.toLong() },
                                onValueChangeFinished = {
                                    val target = dragPositionMs
                                    dragPositionMs = null
                                    if (duration > 0L && target != null) onSeekTo(target)
                                },
                                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                                enabled = duration > 0L
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(displayedPosition),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = if (duration > 0L) {
                                        if (showRemainingTime) {
                                            "-${formatTime((duration - displayedPosition).coerceAtLeast(0L))}"
                                        } else {
                                            formatTime(duration)
                                        }
                                    } else {
                                        "--:--"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.clickable(enabled = duration > 0L) {
                                        showRemainingTime = !showRemainingTime
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(onClick = { onSeekBy(-skipBackSeconds * 1000L) }) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "$skipBackSeconds sn geri",
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Text("-$skipBackSeconds sn", style = MaterialTheme.typography.labelSmall)
                                }

                                FilledIconButton(
                                    onClick = onTogglePlayPause,
                                    shape = morphingPlayShape(playerState.isPlaying),
                                    modifier = Modifier.size(88.dp)
                                ) {
                                    if (playerState.isBuffering) {
                                        LoadingIndicator(
                                            modifier = Modifier.size(44.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (playerState.isPlaying) {
                                                Icons.Default.Pause
                                            } else {
                                                Icons.Default.PlayArrow
                                            },
                                            contentDescription = "Oynat / Duraklat",
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(onClick = { onSeekBy(skipForwardSeconds * 1000L) }) {
                                        Icon(
                                            imageVector = Icons.Default.Forward30,
                                            contentDescription = "$skipForwardSeconds sn ileri",
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Text("+$skipForwardSeconds sn", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalFloatingToolbar(
                                expanded = true,
                                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
                            ) {
                                TextButton(
                                    onClick = {
                                        val presets = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
                                        val idx = presets.indexOf(playerState.playbackSpeed)
                                        onSelectSpeed(presets[(idx + 1) % presets.size])
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = LocalContentColor.current
                                    )
                                ) {
                                    Text(
                                        text = "${playerState.playbackSpeed}x",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                IconButton(onClick = { showOptions = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Bedtime,
                                        contentDescription = "Uyku zamanlayıcı ve hız"
                                    )
                                }
                                IconButton(onClick = { showBookmarks = true }) {
                                    BadgedBox(
                                        badge = {
                                            if (episodeBookmarks.isNotEmpty()) {
                                                Badge { Text(episodeBookmarks.size.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Yer imleri"
                                        )
                                    }
                                }
                                if (episode.description.isNotBlank()) {
                                    IconButton(onClick = { showDescription = true }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Notes,
                                            contentDescription = "Bölüm notları ve içeriği"
                                        )
                                    }
                                }
                                IconButton(onClick = { showQueue = true }) {
                                    BadgedBox(
                                        badge = {
                                            if (queue.isNotEmpty()) {
                                                Badge { Text(queue.size.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = "Çalma sırasını aç"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDescription) {
        EpisodeDescriptionSheet(
            episode = episode,
            onSeekTo = onSeekTo,
            onDismiss = { showDescription = false }
        )
    }

    if (showBookmarks) {
        EpisodeBookmarksSheet(
            episode = episode,
            currentPositionMs = displayedPosition,
            bookmarks = episodeBookmarks,
            onAddBookmark = onAddBookmark,
            onDeleteBookmark = onDeleteBookmark,
            onSeekTo = onSeekTo,
            onDismiss = { showBookmarks = false }
        )
    }

    if (showOptions) {
        PlayerOptionsSheet(
            playbackSpeed = playerState.playbackSpeed,
            onSelectSpeed = onSelectSpeed,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            sleepAtEndOfEpisode = sleepAtEndOfEpisode,
            onStartSleepTimer = onStartSleepTimer,
            onSleepAtEndOfEpisode = onSleepAtEndOfEpisode,
            onCancelSleepTimer = onCancelSleepTimer,
            autoPlayNext = autoPlayNext,
            onSetAutoPlayNext = onSetAutoPlayNext,
            queueSize = queue.size,
            onOpenQueue = {
                showOptions = false
                showQueue = true
            },
            onDismiss = { showOptions = false }
        )
    }

    if (showQueue) {
        QueueSheet(
            queue = queue,
            onPlay = onPlayQueued,
            onRemove = onRemoveFromQueue,
            onMoveUp = onMoveUpInQueue,
            onMoveDown = onMoveDownInQueue,
            onClear = onClearQueue,
            onDismiss = { showQueue = false }
        )
    }
}
