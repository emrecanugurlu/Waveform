package com.cnsoftware.waveform.shared.ui

import androidx.compose.runtime.Composable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ThemePalette { STUDIO, OCEAN, SUNSET, FOREST }

/** Seçilen moda ve sistemin koyu tema durumuna göre koyu temanın kullanılıp kullanılmayacağı. */
internal fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDarkTheme
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Durum ve gezinme çubuğu simgelerinin rengini uygulama temasına uydurur (Android'de gerekli). */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
