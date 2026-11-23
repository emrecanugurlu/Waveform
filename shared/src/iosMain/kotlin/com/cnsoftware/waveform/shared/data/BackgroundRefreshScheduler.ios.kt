package com.cnsoftware.waveform.shared.data

actual class BackgroundRefreshScheduler actual constructor() : RefreshScheduler {
    // iOS arka plan gorevleri (BGTaskScheduler) Info.plist ve native kayit gerektirir; henuz yok.
    actual override val isSupported: Boolean = false

    actual override fun setEnabled(enabled: Boolean) = Unit
}
