package com.cnsoftware.waveform.shared.ui

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

internal actual fun areSystemNotificationsEnabled(): Boolean = true

internal actual fun openNotificationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
}

internal actual fun requestNotificationPermissionIfNeeded() = Unit

internal actual fun appVersionName(): String =
    (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "1.0"
