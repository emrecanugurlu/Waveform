@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cnsoftware.waveform.shared.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.ui.episodeMetaText
import com.cnsoftware.waveform.shared.ui.episodeProgress
import com.cnsoftware.waveform.shared.ui.localUtcOffsetMillis
import com.cnsoftware.waveform.shared.ui.nowMillis
import kotlin.math.PI
import kotlin.math.sin

/** Favori bolumler ve ac/kapa islemi; bolum satirlari parametre zincirine girmeden erissin. */
internal class FavoritesState(val episodes: List<Episode>, val toggle: (Episode) -> Unit) {
    val ids: Set<String> = episodes.mapTo(HashSet()) { it.id }
}

internal val LocalFavorites = compositionLocalOf { FavoritesState(emptyList()) {} }

/** Dinlendi olarak işaretlenen bölüm kimlikleri; listelerde görsel ayrım için. */
internal val LocalPlayedIds = compositionLocalOf<Set<String>> { emptySet() }

internal fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0L) "$hours:$mm:$ss" else "$mm:$ss"
}

/**
 * Canlı ses dalgası göstergesi: çalarken organik faz farklarıyla nefes alır, duraklatıldığında sakin
 * simetrik çubuklara döner.
 */
@Composable
internal fun LiveWaveformBars(
    isPlaying: Boolean,
    barCount: Int = 5,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier.size(width = 26.dp, height = 18.dp)
) {
    val phase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "waveformPhase")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "waveformAngle"
        ).value
    } else {
        0f
    }

    Canvas(modifier = modifier) {
        val count = barCount.coerceAtLeast(3)
        val totalW = size.width
        val totalH = size.height
        val barWidth = totalW / (count * 1.8f)
        val gap = (totalW - barWidth * count) / (count - 1).coerceAtLeast(1)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until count) {
            val factor = if (isPlaying) {
                val offset = i * 0.9f
                val wave = (sin(phase + offset) + 1f) * 0.5f
                0.28f + wave * 0.72f
            } else {
                val distFromCenter = kotlin.math.abs(i - (count - 1) / 2f)
                (0.75f - distFromCenter * 0.16f).coerceIn(0.3f, 0.8f)
            }
            val barH = totalH * factor
            val x = i * (barWidth + gap)
            val y = (totalH - barH) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = radius
            )
        }
    }
}

/** Material 3 Expressive bağlantılı seçim düğme grubu. */
@Composable
internal fun <T> ConnectedToggleRow(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        items.forEachIndexed { index, (value, label) ->
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            TonalToggleButton(
                checked = value == selected,
                onCheckedChange = { onSelect(value) },
                shapes = shapes,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun PodcastArtwork(
    imageUrl: String?,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(cornerRadius)
) {
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Podcasts,
                contentDescription = null,
                modifier = Modifier.size(size * 0.45f),
                tint = MaterialTheme.colorScheme.primary
            )
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                )
            }
        }
    }
}

/** Kapak görselinin köşesine, dinlenmiş bölümler için küçük bir onay rozeti ekler. */
@Composable
internal fun ArtworkWithPlayedBadge(isPlayed: Boolean, artwork: @Composable () -> Unit) {
    Box {
        artwork()
        if (isPlayed) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Dinlendi",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}

/** Oynat/duraklat düğmesi çalarken yumuşak kareye (squircle), durduğunda daireye dönüşür. */
@Composable
internal fun morphingPlayShape(isPlaying: Boolean): Shape {
    val corner by animateDpAsState(
        targetValue = if (isPlaying) 18.dp else 28.dp,
        label = "playShape"
    )
    return RoundedCornerShape(corner)
}

/** Boş durumlar ve istatistik kartları için M3 Expressive şekilli rozet. */
@Composable
internal fun ShapeBadge(
    icon: ImageVector,
    shape: Shape,
    size: Dp = 120.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.46f),
            tint = contentColor
        )
    }
}

/** Kısaltılmış metin; taşıyorsa "Devamını oku" / "Daha az göster" ile açılıp kapanır. */
@Composable
internal fun ExpandableText(
    text: String,
    style: TextStyle,
    collapsedLines: Int,
    modifier: Modifier = Modifier
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = text,
            style = style,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow }
        )
        if (overflows || expanded) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Daha az göster" else "Devamını oku")
            }
        }
    }
}

/** İlk yüklemede yeni hero + carousel + bölüm listesi düzenini yansıtan iskelet görünüm. */
@Composable
internal fun LoadingSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 850), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    val block = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)

    @Composable
    fun Bar(width: Dp, height: Dp = 14.dp, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(10.dp))
                .background(block)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero devam et kartı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(block)
        )
        // Carousel yer tutucusu
        Bar(width = 160.dp, height = 18.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 168.dp, height = 168.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(block)
            )
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 168.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(block.copy(alpha = alpha * 0.75f))
            )
        }
        // Bölüm listesi
        Bar(width = 200.dp, height = 18.dp)
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(block.copy(alpha = alpha * 0.6f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(block)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bar(width = 110.dp, height = 10.dp)
                    Bar(width = 190.dp)
                    Bar(width = 90.dp, height = 10.dp)
                }
            }
        }
    }
}

@Composable
internal fun DownloadProgressIndicator(progress: Float?, size: Dp) {
    // M3 Expressive dalgalı halka ve ortasında iptal simgesi.
    Box(contentAlignment = Alignment.Center) {
        if (progress == null) {
            CircularWavyProgressIndicator(modifier = Modifier.size(size + 6.dp))
        } else {
            CircularWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(size + 6.dp)
            )
        }
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "İndirmeyi iptal et",
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

/** Satırın alt metni (Türkçe tarih • süre) ve kısmen dinlenmişse dalgalı ilerleme çizgisi. */
@Composable
internal fun EpisodeMeta(episode: Episode, savedMs: Long, isPlayed: Boolean, isDownloaded: Boolean) {
    val nowMs = remember { nowMillis() }
    val utcOffsetMs = remember(nowMs) { localUtcOffsetMillis(nowMs) }
    val text = remember(episode.pubDate, episode.durationText, savedMs, isPlayed, utcOffsetMs) {
        episodeMetaText(episode.pubDate, episode.durationText, savedMs, isPlayed, nowMs, utcOffsetMs)
    }
    val progress = if (isPlayed) null else episodeProgress(episode.durationText, savedMs)
    val detailText = if (isPlayed) text.removePrefix("Dinlendi").removePrefix(" • ") else text
    if (text.isNotBlank() || isDownloaded) {
        Row(
            modifier = Modifier.padding(top = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDownloaded) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "İndirildi",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (isPlayed) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Dinlendi",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1
                )
                if (detailText.isNotBlank()) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = detailText,
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
            amplitude = { 0f },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 8.dp)
        )
    }
}

/**
 * Satırın sağ tarafı: oynat/duraklat düğmesi (şekil değiştiren) + "⋮" menüsü (favori, sıra, indirme, dinlendi).
 */
@Composable
internal fun EpisodeRowActions(
    episode: Episode,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isQueued: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    isPlayed: Boolean?,
    onTogglePlayed: (() -> Unit)?,
    onToggleQueue: () -> Unit,
    onToggleDownload: () -> Unit,
    onPlay: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    if (isDownloading) {
        IconButton(onClick = onToggleDownload) {
            DownloadProgressIndicator(progress = downloadProgress, size = 26.dp)
        }
    }
    FilledIconButton(
        onClick = onPlay,
        shape = morphingPlayShape(isCurrent && isPlaying),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (isCurrent) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        ),
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = "Oynat / Duraklat",
            modifier = Modifier.size(22.dp)
        )
    }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Diğer işlemler")
        }
        val favorites = LocalFavorites.current
        val isFavorite = episode.id in favorites.ids
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                    )
                },
                onClick = {
                    menuOpen = false
                    favorites.toggle(episode)
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
                    menuOpen = false
                    onToggleQueue()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        when {
                            isDownloading -> "İndirmeyi iptal et"
                            isDownloaded -> "İndirilenlerden sil"
                            else -> "Çevrimdışı indir"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = when {
                            isDownloading -> Icons.Default.Close
                            isDownloaded -> Icons.Default.DownloadDone
                            else -> Icons.Default.Download
                        },
                        contentDescription = null
                    )
                },
                onClick = {
                    menuOpen = false
                    onToggleDownload()
                }
            )
            if (isPlayed != null && onTogglePlayed != null) {
                DropdownMenuItem(
                    text = { Text(if (isPlayed) "Dinlenmedi olarak işaretle" else "Dinlendi olarak işaretle") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isPlayed) {
                                Icons.Default.RadioButtonUnchecked
                            } else {
                                Icons.Default.CheckCircle
                            },
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onTogglePlayed()
                    }
                )
            }
        }
    }
}

@Composable
internal fun HomeEpisodeCard(
    episode: Episode,
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
    val isCurrent = episode.id == currentlyPlayingId
    val isQueued = episode.id in queuedEpisodeIds
    val isDownloaded = episode.id in downloadedEpisodeIds
    val isDownloading = episode.id in downloadingEpisodeIds
    val isPlayed = episode.id in LocalPlayedIds.current
    val savedMs = episodePositions[episode.id] ?: 0L
    val cardShape = RoundedCornerShape(26.dp)

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                    .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkWithPlayedBadge(isPlayed = isPlayed) {
                    PodcastArtwork(
                        imageUrl = episode.imageUrl,
                        size = 58.dp,
                        cornerRadius = 16.dp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isCurrent) {
                            LiveWaveformBars(
                                isPlaying = isPlaying,
                                barCount = 4,
                                modifier = Modifier.size(width = 16.dp, height = 12.dp)
                            )
                        }
                        Text(
                            text = episode.podcastTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
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

@Composable
internal fun AddRssDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var rssUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("RSS Beslemesi Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Podcast'in RSS XML adresini gir:",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = rssUrl,
                    onValueChange = { rssUrl = it },
                    placeholder = { Text("https://feeds.simplecast.com/...") },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { rssUrl = "https://feeds.megaphone.fm/vergecast" },
                    enabled = !isLoading
                ) {
                    Text("Örnek RSS Doldur (The Vergecast)")
                }
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(rssUrl) },
                enabled = !isLoading && rssUrl.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Ekle")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("İptal")
            }
        }
    )
}

@Composable
internal fun SettingsSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun SettingsActionRow(
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
        )
    }
}
