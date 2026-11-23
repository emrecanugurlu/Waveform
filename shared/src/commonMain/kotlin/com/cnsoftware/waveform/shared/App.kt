@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cnsoftware.waveform.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.EpisodeBookmark
import com.cnsoftware.waveform.shared.model.PlayerState
import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.model.SearchPodcastResult
import com.cnsoftware.waveform.shared.ui.AppTab
import com.cnsoftware.waveform.shared.ui.LibrarySection
import com.cnsoftware.waveform.shared.ui.PlatformBackHandler
import com.cnsoftware.waveform.shared.ui.PodcastTheme
import com.cnsoftware.waveform.shared.ui.PodcastUiState
import com.cnsoftware.waveform.shared.ui.PodcastViewModel
import com.cnsoftware.waveform.shared.ui.SystemBarsAppearance
import com.cnsoftware.waveform.shared.ui.ThemeMode
import com.cnsoftware.waveform.shared.ui.ThemePalette
import com.cnsoftware.waveform.shared.ui.components.AddRssDialog
import com.cnsoftware.waveform.shared.ui.components.FavoritesState
import com.cnsoftware.waveform.shared.ui.components.LoadingSkeleton
import com.cnsoftware.waveform.shared.ui.components.LocalFavorites
import com.cnsoftware.waveform.shared.ui.components.LocalPlayedIds
import com.cnsoftware.waveform.shared.ui.areSystemNotificationsEnabled
import com.cnsoftware.waveform.shared.ui.isDark
import com.cnsoftware.waveform.shared.ui.openNotificationSettings
import com.cnsoftware.waveform.shared.ui.player.FullPlayerScreen
import com.cnsoftware.waveform.shared.ui.player.MiniPlayerBar
import com.cnsoftware.waveform.shared.ui.screens.DiscoverScreen
import com.cnsoftware.waveform.shared.ui.screens.HistoryScreen
import com.cnsoftware.waveform.shared.ui.screens.HomeScreen
import com.cnsoftware.waveform.shared.ui.screens.LibraryScreen
import com.cnsoftware.waveform.shared.ui.screens.NotificationsScreen
import com.cnsoftware.waveform.shared.ui.screens.PodcastDetailScreen
import com.cnsoftware.waveform.shared.ui.screens.SettingsScreen

@Composable
fun App(viewModel: PodcastViewModel = viewModel { PodcastViewModel() }) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    var systemNotificationsEnabled by remember { mutableStateOf(areSystemNotificationsEnabled()) }

    // Arka plan yenilemesi ve sistem bildirim izni uygulama disindayken degismis olabilir.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.reloadFromStore()
        systemNotificationsEnabled = areSystemNotificationsEnabled()
    }

    val darkTheme = uiState.themeMode.isDark(isSystemInDarkTheme())
    SystemBarsAppearance(darkTheme)

    PodcastTheme(darkTheme = darkTheme, palette = uiState.themePalette) {
        CompositionLocalProvider(
            LocalFavorites provides FavoritesState(uiState.favorites, viewModel::toggleFavorite),
            LocalPlayedIds provides uiState.playedEpisodeIds
        ) {
            PodcastAppScaffold(
                uiState = uiState,
                playerState = playerState,
                onSelectTab = viewModel::selectTab,
                onSelectLibrarySection = viewModel::selectLibrarySection,
                onOpenPodcast = viewModel::openPodcastDetail,
                onCloseDetail = viewModel::closePodcastDetail,
                onShowAddRss = viewModel::showAddRssDialog,
                onHideAddRss = viewModel::hideAddRssDialog,
                onAddRss = { url -> viewModel.addPodcastByRss(url, openAfterAdd = true) },
                onRemoveSubscription = viewModel::removeSubscription,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onSearch = viewModel::searchPodcasts,
                onCategorySearch = viewModel::searchByCategory,
                onRemoveRecentSearch = viewModel::removeRecentSearch,
                onClearRecentSearches = viewModel::clearRecentSearches,
                onSubscribeSearchResult = { result ->
                    viewModel.addPodcastByRss(result.feedUrl, openAfterAdd = true)
                },
                onPlayEpisode = viewModel::playEpisode,
                onReplayFromStart = { episode -> viewModel.playEpisodeAt(episode, 0L) },
                onPlayBookmark = viewModel::playBookmark,
                onTogglePlayPause = viewModel::togglePlayPause,
                onToggleDownload = viewModel::toggleDownloadEpisode,
                onRefreshSubscriptions = viewModel::refreshSubscriptions,
                onMarkAllNotificationsRead = viewModel::markAllNotificationsRead,
                onDismissNotification = viewModel::dismissNotification,
                onClearNotifications = viewModel::clearNotifications,
                onOpenPlayer = viewModel::openPlayer,
                onClosePlayer = viewModel::closePlayer,
                onSeekTo = viewModel::seekTo,
                onSeekBy = viewModel::seekBy,
                onSelectSpeed = viewModel::setPlaybackSpeed,
                onAddBookmark = viewModel::addBookmark,
                onDeleteBookmark = viewModel::deleteBookmark,
                onDismissDownloadError = viewModel::dismissDownloadError,
                onDismissStatusMessage = viewModel::dismissStatusMessage,
                onStartSleepTimer = viewModel::startSleepTimer,
                onSleepAtEndOfEpisode = viewModel::sleepAtEndOfEpisode,
                onCancelSleepTimer = viewModel::cancelSleepTimer,
                onSetAutoPlayNext = viewModel::setAutoPlayNext,
                onSetSkipBackSeconds = viewModel::setSkipBackSeconds,
                onSetSkipForwardSeconds = viewModel::setSkipForwardSeconds,
                onToggleQueue = viewModel::toggleQueue,
                onRemoveFromQueue = viewModel::removeFromQueue,
                onMoveUpInQueue = viewModel::moveUpInQueue,
                onMoveDownInQueue = viewModel::moveDownInQueue,
                onClearQueue = viewModel::clearQueue,
                onTogglePlayed = viewModel::togglePlayed,
                onSetPodcastPlayed = viewModel::setPodcastPlayed,
                isBackgroundRefreshSupported = viewModel.isBackgroundRefreshSupported,
                onSetBackgroundRefresh = viewModel::setBackgroundRefresh,
                systemNotificationsEnabled = systemNotificationsEnabled,
                onOpenNotificationSettings = ::openNotificationSettings,
                onDeleteAllDownloads = viewModel::deleteAllDownloads,
                onRemoveHistoryEntry = viewModel::removeFromListeningHistory,
                onClearListeningHistory = viewModel::clearListeningHistory,
                onSetThemeMode = viewModel::setThemeMode,
                onSetThemePalette = viewModel::setThemePalette,
                onExportOpml = viewModel::exportOpml,
                onImportOpml = viewModel::importOpml
            )
        }
    }
}

@Composable
private fun PodcastAppScaffold(
    uiState: PodcastUiState,
    playerState: PlayerState,
    onSelectTab: (AppTab) -> Unit,
    onSelectLibrarySection: (LibrarySection) -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onCloseDetail: () -> Unit,
    onShowAddRss: () -> Unit,
    onHideAddRss: () -> Unit,
    onAddRss: (String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCategorySearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onSubscribeSearchResult: (SearchPodcastResult) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onReplayFromStart: (Episode) -> Unit,
    onPlayBookmark: (EpisodeBookmark) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleDownload: (Episode) -> Unit,
    onRefreshSubscriptions: () -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearNotifications: () -> Unit,
    onOpenPlayer: () -> Unit,
    onClosePlayer: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onAddBookmark: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onDismissDownloadError: () -> Unit,
    onDismissStatusMessage: () -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onSleepAtEndOfEpisode: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetAutoPlayNext: (Boolean) -> Unit,
    onSetSkipBackSeconds: (Int) -> Unit,
    onSetSkipForwardSeconds: (Int) -> Unit,
    onToggleQueue: (Episode) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onMoveUpInQueue: (String) -> Unit,
    onMoveDownInQueue: (String) -> Unit,
    onClearQueue: () -> Unit,
    onTogglePlayed: (Episode) -> Unit,
    onSetPodcastPlayed: (Podcast, Boolean) -> Unit,
    isBackgroundRefreshSupported: Boolean,
    onSetBackgroundRefresh: (Boolean) -> Unit,
    systemNotificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onDeleteAllDownloads: () -> Unit,
    onRemoveHistoryEntry: (String) -> Unit,
    onClearListeningHistory: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetThemePalette: (ThemePalette) -> Unit,
    onExportOpml: () -> String,
    onImportOpml: (String) -> Unit
) {
    val selectedPodcast = uiState.selectedPodcast
    val unreadCount = uiState.notifications.count { !it.isRead }
    val queuedEpisodeIds = remember(uiState.queue) { uiState.queue.map { it.id }.toSet() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.downloadError) {
        val message = uiState.downloadError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissDownloadError()
    }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissStatusMessage()
    }

    var previousTopTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    LaunchedEffect(uiState.activeTab) {
        when (uiState.activeTab) {
            AppTab.HOME, AppTab.DISCOVER, AppTab.LIBRARY, AppTab.NOTIFICATIONS ->
                previousTopTab = uiState.activeTab
            AppTab.SETTINGS, AppTab.HISTORY -> Unit
        }
    }
    val returnTab = if (uiState.activeTab == AppTab.SETTINGS || uiState.activeTab == AppTab.HISTORY) {
        previousTopTab
    } else {
        AppTab.HOME
    }

    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val isLibrarySubscriptions =
        uiState.activeTab == AppTab.LIBRARY && uiState.librarySection == LibrarySection.SUBSCRIPTIONS
    val searchableTab = uiState.activeTab == AppTab.HOME || isLibrarySubscriptions
    val searchActive = searchOpen && selectedPodcast == null && searchableTab
    val canNavigateBack = searchActive || selectedPodcast != null || uiState.activeTab != AppTab.HOME
    PlatformBackHandler(enabled = canNavigateBack) {
        when {
            searchActive -> {
                searchOpen = false
                searchQuery = ""
            }
            selectedPodcast != null -> onCloseDetail()
            else -> onSelectTab(returnTab)
        }
    }

    LaunchedEffect(uiState.activeTab, uiState.librarySection, selectedPodcast) {
        searchOpen = false
        searchQuery = ""
    }
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) runCatching { searchFocus.requestFocus() }
    }

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    LaunchedEffect(uiState.activeTab, uiState.librarySection, selectedPodcast, searchActive) {
        topBarScrollBehavior.state.heightOffset = 0f
        topBarScrollBehavior.state.contentOffset = 0f
    }

    val isTopLevelTab =
        selectedPodcast == null &&
            uiState.activeTab != AppTab.SETTINGS &&
            uiState.activeTab != AppTab.HISTORY &&
            !searchActive
    val headerTitle = selectedPodcast?.title ?: when (uiState.activeTab) {
        AppTab.HOME -> "Waveform"
        AppTab.DISCOVER -> "Keşfet"
        AppTab.LIBRARY -> "Kitaplık"
        AppTab.NOTIFICATIONS -> "Gelen Kutusu"
        AppTab.SETTINGS -> "Ayarlar"
        AppTab.HISTORY -> "Dinleme Geçmişi"
    }
    val headerSubtitle = when (uiState.activeTab) {
        AppTab.HOME -> if (uiState.subscriptions.isEmpty()) {
            "Podcast stüdyona hoş geldin"
        } else {
            "${uiState.subscriptions.size} abonelik • ${uiState.downloadedEpisodeIds.size} çevrimdışı"
        }
        AppTab.DISCOVER -> "Dizinden ara veya RSS ile ekle"
        AppTab.LIBRARY -> "${uiState.subscriptions.size} abonelik • ${uiState.favorites.size} favori"
        AppTab.NOTIFICATIONS -> if (unreadCount > 0) "$unreadCount okunmamış bölüm" else "Tüm bölümler güncel"
        AppTab.SETTINGS, AppTab.HISTORY -> null
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isTopLevelTab) {
                MediumFlexibleTopAppBar(
                    scrollBehavior = topBarScrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    title = {
                        Text(
                            text = headerTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    subtitle = headerSubtitle?.let { sub ->
                        {
                            Text(
                                text = sub,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        val canSearchHome = when (uiState.activeTab) {
                            AppTab.HOME -> uiState.subscriptions.isNotEmpty() || uiState.recentEpisodes.isNotEmpty()
                            AppTab.LIBRARY ->
                                uiState.librarySection == LibrarySection.SUBSCRIPTIONS &&
                                    uiState.subscriptions.isNotEmpty()
                            else -> false
                        }
                        if (canSearchHome) {
                            IconButton(onClick = { searchOpen = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Ara")
                            }
                        }
                        IconButton(onClick = { onSelectTab(AppTab.SETTINGS) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                        }
                    }
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = when {
                                searchActive -> {
                                    {
                                        searchOpen = false
                                        searchQuery = ""
                                    }
                                }
                                selectedPodcast != null -> onCloseDetail
                                else -> {
                                    { onSelectTab(returnTab) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (searchActive) "Aramayı kapat" else "Geri"
                            )
                        }
                    },
                    title = {
                        if (searchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                placeholder = {
                                    Text(
                                        text = if (isLibrarySubscriptions) {
                                            "Aboneliklerde ara"
                                        } else {
                                            "Podcast veya bölüm ara"
                                        },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            Text(
                                text = headerTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (searchActive && searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Aramayı temizle")
                            }
                        } else if (selectedPodcast != null) {
                            IconButton(onClick = { onSelectTab(AppTab.SETTINGS) }) {
                                Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selectedPodcast == null && uiState.activeTab == AppTab.LIBRARY) {
                ExtendedFloatingActionButton(
                    onClick = onShowAddRss,
                    icon = { Icon(Icons.Default.RssFeed, contentDescription = null) },
                    text = { Text("RSS Ekle") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        },
        bottomBar = {
            Column {
                if (playerState.currentEpisode != null) {
                    MiniPlayerBar(
                        playerState = playerState,
                        skipForwardSeconds = uiState.skipForwardSeconds,
                        onTogglePlayPause = onTogglePlayPause,
                        onQuickForward = { onSeekBy(uiState.skipForwardSeconds * 1000L) },
                        onClick = onOpenPlayer
                    )
                }
                val navDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(navDensity.density, navDensity.fontScale.coerceAtMost(1.15f))
                ) {
                    ShortNavigationBar {
                        ShortNavigationBarItem(
                            selected = uiState.activeTab == AppTab.HOME,
                            onClick = { onSelectTab(AppTab.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Ana Sayfa", maxLines = 1, softWrap = false) }
                        )
                        ShortNavigationBarItem(
                            selected = uiState.activeTab == AppTab.DISCOVER,
                            onClick = { onSelectTab(AppTab.DISCOVER) },
                            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                            label = { Text("Keşfet", maxLines = 1, softWrap = false) }
                        )
                        ShortNavigationBarItem(
                            selected = uiState.activeTab == AppTab.LIBRARY,
                            onClick = { onSelectTab(AppTab.LIBRARY) },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                            label = { Text("Kitaplık", maxLines = 1, softWrap = false) }
                        )
                        ShortNavigationBarItem(
                            selected = uiState.activeTab == AppTab.NOTIFICATIONS,
                            onClick = { onSelectTab(AppTab.NOTIFICATIONS) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge { Text(unreadCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null)
                                }
                            },
                            label = { Text("Bildirimler", maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingSkeleton()

                uiState.activeTab == AppTab.SETTINGS && selectedPodcast == null -> {
                    SettingsScreen(
                        autoPlayNext = uiState.autoPlayNext,
                        onSetAutoPlayNext = onSetAutoPlayNext,
                        skipBackSeconds = uiState.skipBackSeconds,
                        onSetSkipBackSeconds = onSetSkipBackSeconds,
                        skipForwardSeconds = uiState.skipForwardSeconds,
                        onSetSkipForwardSeconds = onSetSkipForwardSeconds,
                        isBackgroundRefreshSupported = isBackgroundRefreshSupported,
                        backgroundRefreshEnabled = uiState.backgroundRefreshEnabled,
                        onSetBackgroundRefresh = onSetBackgroundRefresh,
                        systemNotificationsEnabled = systemNotificationsEnabled,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        downloadedCount = uiState.downloadedEpisodeIds.size,
                        onDeleteAllDownloads = onDeleteAllDownloads,
                        hasListeningHistory = uiState.playedEpisodeIds.isNotEmpty() ||
                            uiState.episodePositions.isNotEmpty() ||
                            uiState.recentEpisodes.isNotEmpty() ||
                            uiState.listeningHistory.isNotEmpty() ||
                            uiState.listeningStats.totalListeningSeconds > 0L ||
                            uiState.listeningStats.completedEpisodesCount > 0,
                        onClearListeningHistory = onClearListeningHistory,
                        themeMode = uiState.themeMode,
                        onSetThemeMode = onSetThemeMode,
                        themePalette = uiState.themePalette,
                        onSetThemePalette = onSetThemePalette,
                        subscriptionsCount = uiState.subscriptions.size,
                        isImportingOpml = uiState.isImportingOpml,
                        onExportOpml = onExportOpml,
                        onImportOpml = onImportOpml
                    )
                }

                uiState.activeTab == AppTab.HISTORY && selectedPodcast == null -> {
                    HistoryScreen(
                        history = uiState.listeningHistory,
                        listeningStats = uiState.listeningStats,
                        subscriptions = uiState.subscriptions,
                        episodePositions = uiState.episodePositions,
                        playedEpisodeIds = uiState.playedEpisodeIds,
                        queuedEpisodeIds = queuedEpisodeIds,
                        currentlyPlayingId = playerState.currentEpisode?.id,
                        isPlaying = playerState.isPlaying,
                        onPlayEpisode = onPlayEpisode,
                        onReplayFromStart = onReplayFromStart,
                        onTogglePlayPause = onTogglePlayPause,
                        onToggleQueue = onToggleQueue,
                        onTogglePlayed = onTogglePlayed,
                        onRemoveHistoryEntry = onRemoveHistoryEntry,
                        onClearHistory = onClearListeningHistory,
                        onOpenPodcast = onOpenPodcast,
                        onExploreClick = { onSelectTab(AppTab.DISCOVER) }
                    )
                }

                selectedPodcast != null -> {
                    PodcastDetailScreen(
                        podcast = selectedPodcast,
                        episodePositions = uiState.episodePositions,
                        downloadedEpisodeIds = uiState.downloadedEpisodeIds,
                        downloadingEpisodeIds = uiState.downloadingEpisodeIds,
                        downloadProgress = uiState.downloadProgress,
                        queuedEpisodeIds = queuedEpisodeIds,
                        playedEpisodeIds = uiState.playedEpisodeIds,
                        currentlyPlayingId = playerState.currentEpisode?.id,
                        isPlaying = playerState.isPlaying,
                        onToggleQueue = onToggleQueue,
                        onTogglePlayed = onTogglePlayed,
                        onSetPodcastPlayed = onSetPodcastPlayed,
                        onRemoveSubscription = onRemoveSubscription,
                        onPlayEpisode = onPlayEpisode,
                        onTogglePlayPause = onTogglePlayPause,
                        onToggleDownload = onToggleDownload
                    )
                }

                uiState.activeTab == AppTab.HOME -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshingSubscriptions,
                    onRefresh = onRefreshSubscriptions
                ) {
                    HomeScreen(
                        query = searchQuery,
                        subscriptions = uiState.subscriptions,
                        recentEpisodes = uiState.recentEpisodes,
                        notifications = uiState.notifications,
                        episodePositions = uiState.episodePositions,
                        downloadedEpisodeIds = uiState.downloadedEpisodeIds,
                        downloadingEpisodeIds = uiState.downloadingEpisodeIds,
                        downloadProgress = uiState.downloadProgress,
                        queuedEpisodeIds = queuedEpisodeIds,
                        playedEpisodeIds = uiState.playedEpisodeIds,
                        bookmarksCount = uiState.bookmarks.size,
                        historyCount = uiState.listeningHistory.size,
                        skipForwardSeconds = uiState.skipForwardSeconds,
                        currentlyPlayingId = playerState.currentEpisode?.id,
                        isPlaying = playerState.isPlaying,
                        onToggleQueue = onToggleQueue,
                        onOpenPodcast = onOpenPodcast,
                        onPlayEpisode = onPlayEpisode,
                        onTogglePlayPause = onTogglePlayPause,
                        onSeekBy = onSeekBy,
                        onToggleDownload = onToggleDownload,
                        onSearchClick = { onSelectTab(AppTab.DISCOVER) },
                        onAddRssClick = onShowAddRss,
                        onOpenDownloads = { onSelectLibrarySection(LibrarySection.DOWNLOADS) },
                        onOpenFavorites = { onSelectLibrarySection(LibrarySection.FAVORITES) },
                        onOpenBookmarks = { onSelectLibrarySection(LibrarySection.BOOKMARKS) },
                        onOpenHistory = { onSelectTab(AppTab.HISTORY) },
                        onOpenPlayer = onOpenPlayer
                    )
                }

                uiState.activeTab == AppTab.LIBRARY -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshingSubscriptions,
                    onRefresh = onRefreshSubscriptions
                ) {
                    LibraryScreen(
                        section = uiState.librarySection,
                        query = searchQuery,
                        subscriptions = uiState.subscriptions,
                        recentEpisodes = uiState.recentEpisodes,
                        notifications = uiState.notifications,
                        favorites = uiState.favorites,
                        bookmarks = uiState.bookmarks,
                        queue = uiState.queue,
                        listeningStats = uiState.listeningStats,
                        episodePositions = uiState.episodePositions,
                        downloadedEpisodeIds = uiState.downloadedEpisodeIds,
                        downloadingEpisodeIds = uiState.downloadingEpisodeIds,
                        downloadProgress = uiState.downloadProgress,
                        queuedEpisodeIds = queuedEpisodeIds,
                        playedEpisodeIds = uiState.playedEpisodeIds,
                        currentlyPlayingId = playerState.currentEpisode?.id,
                        isPlaying = playerState.isPlaying,
                        onSelectSection = onSelectLibrarySection,
                        onOpenPodcast = onOpenPodcast,
                        onSetPodcastPlayed = onSetPodcastPlayed,
                        onRemoveSubscription = onRemoveSubscription,
                        onAddRssClick = onShowAddRss,
                        onSearchClick = { onSelectTab(AppTab.DISCOVER) },
                        onToggleQueue = onToggleQueue,
                        onPlayEpisode = onPlayEpisode,
                        onPlayBookmark = onPlayBookmark,
                        onDeleteBookmark = onDeleteBookmark,
                        onTogglePlayPause = onTogglePlayPause,
                        onToggleDownload = onToggleDownload,
                        onOpenHistory = { onSelectTab(AppTab.HISTORY) }
                    )
                }

                uiState.activeTab == AppTab.NOTIFICATIONS -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshingSubscriptions,
                    onRefresh = onRefreshSubscriptions
                ) {
                    NotificationsScreen(
                        notifications = uiState.notifications,
                        refreshMessage = uiState.refreshMessage,
                        hasSubscriptions = uiState.subscriptions.isNotEmpty(),
                        currentlyPlayingId = playerState.currentEpisode?.id,
                        isPlaying = playerState.isPlaying,
                        episodePositions = uiState.episodePositions,
                        downloadedEpisodeIds = uiState.downloadedEpisodeIds,
                        downloadingEpisodeIds = uiState.downloadingEpisodeIds,
                        downloadProgress = uiState.downloadProgress,
                        queuedEpisodeIds = queuedEpisodeIds,
                        onToggleQueue = onToggleQueue,
                        onToggleDownload = onToggleDownload,
                        onMarkAllRead = onMarkAllNotificationsRead,
                        onDismissNotification = onDismissNotification,
                        onClearNotifications = onClearNotifications,
                        onPlayEpisode = onPlayEpisode,
                        onTogglePlayPause = onTogglePlayPause
                    )
                }

                uiState.activeTab == AppTab.DISCOVER -> {
                    DiscoverScreen(
                        query = uiState.searchQuery,
                        results = uiState.searchResults,
                        recentSearches = uiState.recentSearches,
                        subscribedFeedUrls = uiState.subscriptions.map { it.feedUrl }.toSet(),
                        addingFeedUrl = uiState.addingFeedUrl,
                        hasSearched = uiState.hasSearched,
                        isSearching = uiState.isSearching,
                        error = uiState.searchError,
                        onQueryChange = onSearchQueryChange,
                        onSearch = onSearch,
                        onCategoryClick = onCategorySearch,
                        onRemoveRecentSearch = onRemoveRecentSearch,
                        onClearRecentSearches = onClearRecentSearches,
                        onSubscribe = onSubscribeSearchResult,
                        onSubscribeFeedUrl = onAddRss,
                        onAddRss = onShowAddRss
                    )
                }
            }
        }
    }

    if (uiState.isAddRssDialogVisible) {
        AddRssDialog(
            isLoading = uiState.isAddingRss,
            error = uiState.rssError,
            onDismiss = onHideAddRss,
            onAdd = onAddRss
        )
    }

    if (uiState.isPlayerExpanded && playerState.currentEpisode != null) {
        FullPlayerScreen(
            playerState = playerState,
            skipBackSeconds = uiState.skipBackSeconds,
            skipForwardSeconds = uiState.skipForwardSeconds,
            bookmarks = uiState.bookmarks,
            onAddBookmark = onAddBookmark,
            onDeleteBookmark = onDeleteBookmark,
            onClose = onClosePlayer,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSeekBy = onSeekBy,
            onSelectSpeed = onSelectSpeed,
            sleepTimerRemainingMs = uiState.sleepTimerRemainingMs,
            sleepAtEndOfEpisode = uiState.sleepAtEndOfEpisode,
            onStartSleepTimer = onStartSleepTimer,
            onSleepAtEndOfEpisode = onSleepAtEndOfEpisode,
            onCancelSleepTimer = onCancelSleepTimer,
            autoPlayNext = uiState.autoPlayNext,
            onSetAutoPlayNext = onSetAutoPlayNext,
            queue = uiState.queue,
            onPlayQueued = onPlayEpisode,
            onRemoveFromQueue = onRemoveFromQueue,
            onMoveUpInQueue = onMoveUpInQueue,
            onMoveDownInQueue = onMoveDownInQueue,
            onClearQueue = onClearQueue
        )
    }
}
