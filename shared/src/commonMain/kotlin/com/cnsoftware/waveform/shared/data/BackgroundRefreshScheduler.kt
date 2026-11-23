package com.cnsoftware.waveform.shared.data

interface RefreshScheduler {
    val isSupported: Boolean
    fun setEnabled(enabled: Boolean)
}

/**
 * Uygulama kapaliyken abonelikleri periyodik yenileyip yeni bolum bildirimi gosterir.
 * Android'de WorkManager ile calisir; iOS'ta henuz desteklenmez ([isSupported] false).
 */
expect class BackgroundRefreshScheduler() : RefreshScheduler {
    override val isSupported: Boolean
    override fun setEnabled(enabled: Boolean)
}
