package com.cnsoftware.waveform.shared.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.cnsoftware.waveform.shared.player.PlatformContextHolder

object NotificationPermissionBridge {
    private var permissionRequester: (() -> Unit)? = null

    fun register(handler: () -> Unit) {
        permissionRequester = handler
    }

    fun unregister(handler: () -> Unit) {
        if (permissionRequester === handler) {
            permissionRequester = null
        }
    }

    fun request() {
        permissionRequester?.invoke()
    }
}

internal actual fun areSystemNotificationsEnabled(): Boolean =
    PlatformContextHolder.getOrNull()
        ?.let { NotificationManagerCompat.from(it).areNotificationsEnabled() }
        ?: true

internal actual fun openNotificationSettings() {
    val context = PlatformContextHolder.getOrNull() ?: return
    val primary = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(primary) }.onFailure {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}

internal actual fun requestNotificationPermissionIfNeeded() {
    NotificationPermissionBridge.request()
}

internal actual fun appVersionName(): String {
    val context = PlatformContextHolder.getOrNull() ?: return "1.0"
    return runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName?.takeIf { it.isNotBlank() } ?: "1.0"
    }.getOrDefault("1.0")
}
