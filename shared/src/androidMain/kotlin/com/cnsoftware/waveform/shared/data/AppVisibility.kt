package com.cnsoftware.waveform.shared.data

/** Uygulamanin ekranda gorunup gorunmedigi; MainActivity onStart/onStop'ta gunceller. */
object AppVisibility {
    @Volatile
    var isForeground: Boolean = false
}
