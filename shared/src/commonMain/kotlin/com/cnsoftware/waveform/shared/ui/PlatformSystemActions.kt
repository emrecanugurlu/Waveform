package com.cnsoftware.waveform.shared.ui

internal expect fun areSystemNotificationsEnabled(): Boolean

internal expect fun openNotificationSettings()

internal expect fun requestNotificationPermissionIfNeeded()

internal expect fun appVersionName(): String
