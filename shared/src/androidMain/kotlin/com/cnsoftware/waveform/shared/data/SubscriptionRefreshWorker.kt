package com.cnsoftware.waveform.shared.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cnsoftware.waveform.shared.player.PlatformContextHolder
import kotlinx.coroutines.CancellationException

/** Uygulama ekranda degilken abonelikleri yeniler ve yeni bolum varsa bildirim gosterir. */
class SubscriptionRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private companion object {
        const val TAG = "SubscriptionRefresh"
    }

    override suspend fun doWork(): Result {
        // Uygulama aciksa ViewModel kendi durumunu yonetir; dosyalara ayni anda yazmayalim.
        if (AppVisibility.isForeground) return Result.success()

        // Surec yalnizca WorkManager tarafindan baslatilmis olabilir (MainActivity calismadi).
        PlatformContextHolder.init(applicationContext)

        val store = PodcastStore()
        val subscriptions = store.loadSubscriptions()
        if (subscriptions.isEmpty()) return Result.success()

        val repository = PodcastRepository()
        return try {
            val results = repository.fetchAll(subscriptions)
            // Hicbir feed indirilemediyse (cevrimdisi vb.) sonra tekrar dene.
            if (results.all { it.second == null }) return Result.retry()

            // Ag istegi surerken kullanici uygulamayi acmissa on plan durumunu ezme.
            if (AppVisibility.isForeground) return Result.success()

            val latestSubscriptions = store.loadSubscriptions()
            val latestNotifications = store.loadNotifications()
            val outcome = reconcileRefresh(results, latestSubscriptions, latestNotifications)

            if (AppVisibility.isForeground) return Result.success()

            store.saveSubscriptions(outcome.subscriptions)
            store.saveNotifications(outcome.notifications)
            if (outcome.newNotifications.isNotEmpty()) {
                NewEpisodeNotifier.show(applicationContext, outcome.newNotifications)
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Arka plan yenileme basarisiz, tekrar denenecek", error)
            Result.retry()
        } finally {
            repository.close()
        }
    }
}
