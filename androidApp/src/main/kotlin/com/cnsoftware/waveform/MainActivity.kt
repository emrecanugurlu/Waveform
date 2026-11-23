package com.cnsoftware.waveform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.cnsoftware.waveform.shared.App
import com.cnsoftware.waveform.shared.data.AppVisibility
import com.cnsoftware.waveform.shared.player.PlatformContextHolder
import com.cnsoftware.waveform.shared.ui.NotificationPermissionBridge

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sonuc sistem tarafinda saklanir */ }
    private val notificationPermissionHandler: () -> Unit = { requestNotificationPermissionOnce() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformContextHolder.init(applicationContext)
        NotificationPermissionBridge.register(notificationPermissionHandler)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.isForeground = true
    }

    override fun onStop() {
        AppVisibility.isForeground = false
        super.onStop()
    }

    override fun onDestroy() {
        NotificationPermissionBridge.unregister(notificationPermissionHandler)
        super.onDestroy()
    }

    /** Yeni bolum bildirimleri icin Android 13+ izni; ilk abonelik veya yenileme sirasinda bir kez sorulur. */
    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return

        val prefs = getSharedPreferences("podcastapp_prefs", MODE_PRIVATE)
        if (prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)) return
        prefs.edit { putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true) }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
    }
}
