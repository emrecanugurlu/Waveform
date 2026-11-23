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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cnsoftware.waveform.shared.model.SearchPodcastResult
import com.cnsoftware.waveform.shared.ui.components.PodcastArtwork
import com.cnsoftware.waveform.shared.ui.looksLikeFeedUrl

private data class DiscoverCategory(
    val title: String,
    val query: String,
    val icon: ImageVector,
    val useTertiary: Boolean
)

private data class StarterPick(
    val title: String,
    val subtitle: String,
    val feedUrl: String
)

private val DISCOVER_CATEGORIES = listOf(
    DiscoverCategory("Teknoloji", "teknoloji", Icons.Default.Code, false),
    DiscoverCategory("Bilim", "bilim", Icons.Default.Science, true),
    DiscoverCategory("Tarih", "tarih", Icons.Default.History, false),
    DiscoverCategory("Kültür & Sanat", "kültür sanat", Icons.Default.TheaterComedy, true),
    DiscoverCategory("Tasarım", "design podcast", Icons.Default.Palette, false),
    DiscoverCategory("Girişim", "girişimcilik", Icons.Default.BusinessCenter, true),
    DiscoverCategory("Haberler", "haber podcast", Icons.Default.Newspaper, false),
    DiscoverCategory("Felsefe", "felsefe", Icons.Default.Psychology, true)
)

private val STARTER_PICKS = listOf(
    StarterPick(
        title = "The Vergecast",
        subtitle = "Teknoloji dünyasından haftalık yorumlar",
        feedUrl = "https://feeds.megaphone.fm/vergecast"
    ),
    StarterPick(
        title = "Syntax.fm",
        subtitle = "Modern web geliştirme ve yazılım sohbetleri",
        feedUrl = "https://feed.syntax.fm/rss"
    ),
    StarterPick(
        title = "99% Invisible",
        subtitle = "Tasarım, mimari ve görünmeyen hikâyeler",
        feedUrl = "https://feeds.simplecast.com/BqbsxVfO"
    )
)

@Composable
internal fun DiscoverScreen(
    query: String,
    results: List<SearchPodcastResult>,
    recentSearches: List<String>,
    subscribedFeedUrls: Set<String>,
    addingFeedUrl: String?,
    hasSearched: Boolean,
    isSearching: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onSubscribe: (SearchPodcastResult) -> Unit,
    onSubscribeFeedUrl: (String) -> Unit,
    onAddRss: () -> Unit
) {
    val isUrlInput = looksLikeFeedUrl(query)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Podcast ara veya RSS adresi yapıştır") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle")
                            }
                        }
                        FilledIconButton(
                            onClick = onSearch,
                            enabled = !isSearching && query.isNotBlank(),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isUrlInput) Icons.Default.RssFeed else Icons.Default.Search,
                                contentDescription = if (isUrlInput) "RSS Ekle" else "Ara"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
        }

        if (isUrlInput) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RssFeed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RSS bağlantısı algılandı — eklemek için düğmeye bas.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isSearching && results.isNotEmpty()) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            isSearching && results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator(modifier = Modifier.size(72.dp))
                }
            }

            error != null -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            hasSearched && results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Eşleşen podcast bulunamadı.",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(onClick = onAddRss) {
                            Icon(Icons.Default.RssFeed, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RSS bağlantısıyla ekle")
                        }
                    }
                }
            }

            results.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results, key = { it.feedUrl }) { item ->
                        val isSubscribed = item.feedUrl in subscribedFeedUrls
                        val isAddingThis = item.feedUrl == addingFeedUrl

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PodcastArtwork(
                                    imageUrl = item.artworkUrl,
                                    size = 56.dp,
                                    cornerRadius = 14.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.author.isNotBlank()) {
                                        Text(
                                            text = item.author,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = { onSubscribe(item) },
                                    enabled = !isSubscribed && addingFeedUrl == null,
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    when {
                                        isAddingThis -> CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        isSubscribed -> {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Eklendi")
                                        }
                                        else -> {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ekle")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "add-by-rss") {
                        TextButton(onClick = onAddRss, modifier = Modifier.fillMaxWidth()) {
                            Text("Aradığını bulamadın mı? RSS bağlantısıyla ekle")
                        }
                    }
                }
            }

            else -> {
                val categoryShapes: List<Shape> = listOf(
                    MaterialShapes.Cookie6Sided.toShape(),
                    MaterialShapes.SoftBurst.toShape(),
                    MaterialShapes.Clover4Leaf.toShape(),
                    MaterialShapes.Sunny.toShape(),
                    MaterialShapes.Gem.toShape(),
                    MaterialShapes.Pill.toShape(),
                    MaterialShapes.Arch.toShape(),
                    MaterialShapes.Flower.toShape()
                )
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Son Aramalar", style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = onClearRecentSearches) {
                                        Text("Temizle")
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(recentSearches, key = { it }) { recent ->
                                        InputChip(
                                            selected = false,
                                            onClick = { onCategoryClick(recent) },
                                            label = { Text(recent) },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Kaldır",
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable { onRemoveRecentSearch(recent) }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Kategoriler", style = MaterialTheme.typography.titleMedium)
                            DISCOVER_CATEGORIES.chunked(2).forEachIndexed { rowIdx, pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEachIndexed { colIdx, cat ->
                                        val index = rowIdx * 2 + colIdx
                                        val container = if (cat.useTertiary) {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        }
                                        val contentColor = if (cat.useTertiary) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onCategoryClick(cat.query) },
                                            shape = RoundedCornerShape(24.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = container,
                                                contentColor = contentColor
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(categoryShapes[index % categoryShapes.size])
                                                        .background(contentColor.copy(alpha = 0.14f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = cat.icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = cat.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 2,
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
                        Text("Başlangıç Önerileri", style = MaterialTheme.typography.titleMedium)
                    }

                    items(STARTER_PICKS, key = { it.feedUrl }) { pick ->
                        val isSubscribed = pick.feedUrl in subscribedFeedUrls
                        val isAddingThis = pick.feedUrl == addingFeedUrl
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(MaterialShapes.Cookie9Sided.toShape())
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pick.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = pick.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = { onSubscribeFeedUrl(pick.feedUrl) },
                                    enabled = !isSubscribed && addingFeedUrl == null,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    when {
                                        isAddingThis -> CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        isSubscribed -> Text("Eklendi")
                                        else -> Text("Abone Ol")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onAddRss),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.RssFeed, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Özel RSS Bağlantısı Ekle",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Dizinde olmayan bağımsız bir podcast'in XML adresini yapıştır.",
                                        style = MaterialTheme.typography.bodySmall
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
