package com.cnsoftware.waveform.shared.data

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cnsoftware.waveform.shared.R
import com.cnsoftware.waveform.shared.model.NewEpisodeNotification

internal object NewEpisodeNotifier {
    private const val CHANNEL_ID = "new_episodes"
    private const val NOTIFICATION_ID = 1001
    private const val MAX_LINES = 5

    @SuppressLint("MissingPermission") // areNotificationsEnabled() asagida kontrol ediliyor.
    fun show(context: Context, newEpisodes: List<NewEpisodeNotification>) {
        if (newEpisodes.isEmpty()) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)

        val first = newEpisodes.first().episode
        val title = if (newEpisodes.size == 1) first.podcastTitle else "${newEpisodes.size} yeni bölüm"
        val text = if (newEpisodes.size == 1) first.title else "${first.podcastTitle} ve diğerleri"

        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

        // Tek bolumde baslik zaten podcast adi; ayni metni liste halinde tekrarlama.
        val style: NotificationCompat.Style = if (newEpisodes.size == 1) {
            NotificationCompat.BigTextStyle().bigText(first.title)
        } else {
            NotificationCompat.InboxStyle().also { inbox ->
                newEpisodes.take(MAX_LINES).forEach { inbox.addLine("${it.episode.podcastTitle}: ${it.episode.title}") }
                if (newEpisodes.size > MAX_LINES) inbox.setSummaryText("+${newEpisodes.size - MAX_LINES} bölüm daha")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_new_episode)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        // Sabit kimlik: yeni bildirim eskisinin yerine gecer, ust uste yigilmaz.
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Yeni bölümler",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Abone olunan podcastlerin yeni bölümleri" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
