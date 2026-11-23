package com.cnsoftware.waveform.shared.data

import com.cnsoftware.waveform.shared.model.NewEpisodeNotification
import com.cnsoftware.waveform.shared.model.Podcast
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal data class RefreshOutcome(
    val subscriptions: List<Podcast>,
    /** Yeni bulunanlar basta olmak uzere tum bildirimler. */
    val notifications: List<NewEpisodeNotification>,
    /** Yalnizca bu yenilemede bulunan yeni bolum bildirimleri. */
    val newNotifications: List<NewEpisodeNotification>
)

/** Her aboneligi paralel indirir; indirilemeyenler icin ikinci deger null olur. */
internal suspend fun PodcastRepository.fetchAll(
    subscriptions: List<Podcast>
): List<Pair<Podcast, Podcast?>> = withContext(Dispatchers.Default) {
    coroutineScope {
        subscriptions.map { existing ->
            async { existing to fetchPodcastByRss(existing.feedUrl).getOrNull() }
        }.awaitAll()
    }
}

/**
 * Yenileme sonuclarini birlestirir. Indirilemeyen abonelik oldugu gibi korunur; daha once
 * bildirimi olan bolumler yeniden bildirilmez.
 */
internal fun mergeRefresh(
    results: List<Pair<Podcast, Podcast?>>,
    currentNotifications: List<NewEpisodeNotification>
): RefreshOutcome {
    val discovered = mutableListOf<NewEpisodeNotification>()
    val subscriptions = results.map { (existing, fresh) ->
        if (fresh == null) return@map existing
        val oldIds = existing.episodes.map { it.id }.toSet()
        fresh.episodes
            .filter { it.id !in oldIds }
            .forEach { episode ->
                discovered += NewEpisodeNotification(
                    episode = episode,
                    podcastFeedUrl = fresh.feedUrl,
                    isRead = false
                )
            }
        fresh
    }

    val knownIds = currentNotifications.map { it.episode.id }.toSet()
    val newNotifications = discovered.filter { it.episode.id !in knownIds }
    return RefreshOutcome(
        subscriptions = subscriptions,
        notifications = newNotifications + currentNotifications,
        newNotifications = newNotifications
    )
}

/**
 * Ağdan gelen yenileme sonuçlarını, yenileme sırasında değişmiş olabilecek güncel abonelik
 * listesiyle uzlaştırır: çıkarılan abonelikler geri gelmez, yeni eklenenler korunur.
 */
internal fun reconcileRefresh(
    results: List<Pair<Podcast, Podcast?>>,
    latestSubscriptions: List<Podcast>,
    currentNotifications: List<NewEpisodeNotification>
): RefreshOutcome {
    val freshByFeed = results.associate { (existing, fresh) -> existing.feedUrl to fresh }
    val aligned = latestSubscriptions.map { latest -> latest to freshByFeed[latest.feedUrl] }
    return mergeRefresh(aligned, currentNotifications)
}
