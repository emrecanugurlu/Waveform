@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cnsoftware.waveform.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.ui.components.ShapeBadge

@Composable
internal fun SubscriptionsScreen(
    query: String,
    subscriptions: List<Podcast>,
    playedEpisodeIds: Set<String>,
    onOpenPodcast: (Podcast) -> Unit,
    onSetPodcastPlayed: (Podcast, Boolean) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onAddRssClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    if (subscriptions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShapeBadge(
                icon = Icons.Default.Podcasts,
                shape = MaterialShapes.Clover8Leaf.toShape()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Henüz podcast eklenmedi",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Keşfet'ten arama yaparak veya RSS bağlantısı yapıştırarak abonelik ekleyebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onSearchClick, shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.Explore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Podcast Keşfet")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onAddRssClick) {
                Text("RSS bağlantısıyla ekle")
            }
        }
        return
    }

    val filtered = remember(subscriptions, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            subscriptions
        } else {
            subscriptions.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.author.contains(q, ignoreCase = true)
            }
        }
    }

    if (filtered.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\"${query.trim()}\" ile eşleşen abonelik bulunamadı.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filtered, key = { it.feedUrl }) { podcast ->
            var menuExpanded by remember { mutableStateOf(false) }
            val unplayedCount = remember(podcast, playedEpisodeIds) {
                podcast.episodes.count { it.id !in playedEpisodeIds }
            }
            val allPlayed = podcast.episodes.isNotEmpty() && unplayedCount == 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPodcast(podcast) },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Podcasts,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                if (!podcast.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = podcast.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(22.dp))
                                    )
                                }
                            }
                        }
                        if (unplayedCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "$unplayedCount yeni",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Seçenekler",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                if (podcast.episodes.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (allPlayed) "Dinlendi işaretini kaldır" else "Tümünü dinlendi say"
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
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
                                        menuExpanded = false
                                        onRemoveSubscription(podcast.feedUrl)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = podcast.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(podcast.author, "${podcast.episodes.size} bölüm")
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
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
