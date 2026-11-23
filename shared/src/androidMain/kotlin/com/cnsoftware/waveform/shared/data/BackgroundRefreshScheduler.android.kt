package com.cnsoftware.waveform.shared.data

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cnsoftware.waveform.shared.player.PlatformContextHolder
import java.util.concurrent.TimeUnit

actual class BackgroundRefreshScheduler actual constructor() : RefreshScheduler {
    actual override val isSupported: Boolean = true

    actual override fun setEnabled(enabled: Boolean) {
        val workManager = WorkManager.getInstance(PlatformContextHolder.get())
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<SubscriptionRefreshWorker>(
            REFRESH_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        // KEEP: uygulama her acildiginda zamanlamayi sifirlama.
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "subscription_refresh"
        const val REFRESH_INTERVAL_HOURS = 6L
    }
}
