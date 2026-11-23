package com.cnsoftware.waveform.shared.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Material 3 Expressive çoklu palet sistemi:
 * - STUDIO (varsayılan): canlı çivit-mor (#5C4CE6) + sıcak mercan vurgusu
 * - OCEAN: derin deniz mavisi (#124E78) + yumuşak leylak vurgusu
 * - SUNSET: sıcak terra/kehribar (#BA4B2F) + altın vurgusu
 * - FOREST: zümrüt yeşili (#1E6B52) + adaçayı vurgusu
 */

private val StudioDarkColors = darkColorScheme(
    primary = Color(0xFFC7BFFF),
    onPrimary = Color(0xFF281780),
    primaryContainer = Color(0xFF4231B5),
    onPrimaryContainer = Color(0xFFE5DFFF),
    secondary = Color(0xFFC8C3DC),
    onSecondary = Color(0xFF302E41),
    secondaryContainer = Color(0xFF474459),
    onSecondaryContainer = Color(0xFFE4DFF8),
    tertiary = Color(0xFFFFB4A8),
    onTertiary = Color(0xFF5D1510),
    tertiaryContainer = Color(0xFF7D2C24),
    onTertiaryContainer = Color(0xFFFFDAD4),
    background = Color(0xFF111018),
    onBackground = Color(0xFFE4E1EC),
    surface = Color(0xFF111018),
    onSurface = Color(0xFFE4E1EC),
    surfaceVariant = Color(0xFF464554),
    onSurfaceVariant = Color(0xFFC7C4D6),
    surfaceContainerLowest = Color(0xFF0C0B12),
    surfaceContainerLow = Color(0xFF191822),
    surfaceContainer = Color(0xFF1E1C27),
    surfaceContainerHigh = Color(0xFF282632),
    surfaceContainerHighest = Color(0xFF33313E)
)

private val StudioLightColors = lightColorScheme(
    primary = Color(0xFF5C4CE6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DFFF),
    onPrimaryContainer = Color(0xFF180468),
    secondary = Color(0xFF5E5B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4DFF8),
    onSecondaryContainer = Color(0xFF1B192B),
    tertiary = Color(0xFFB44337),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF3F0704),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B23),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B23),
    surfaceVariant = Color(0xFFE4E0F0),
    onSurfaceVariant = Color(0xFF464554),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F1FC),
    surfaceContainer = Color(0xFFEEEAF7),
    surfaceContainerHigh = Color(0xFFE8E4F1),
    surfaceContainerHighest = Color(0xFFE2DEEB)
)

private val OceanDarkColors = darkColorScheme(
    primary = Color(0xFF9DCBF8),
    onPrimary = Color(0xFF003355),
    primaryContainer = Color(0xFF124E78),
    onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Color(0xFFB8C8DA),
    onSecondary = Color(0xFF233240),
    secondaryContainer = Color(0xFF394857),
    onSecondaryContainer = Color(0xFFD4E4F6),
    tertiary = Color(0xFF9CD4C6),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF1B5046),
    onTertiaryContainer = Color(0xFFB8F0E2),
    background = Color(0xFF0E1419),
    onBackground = Color(0xFFDEE3E9),
    surface = Color(0xFF0E1419),
    onSurface = Color(0xFFDEE3E9),
    surfaceVariant = Color(0xFF404850),
    onSurfaceVariant = Color(0xFFC0C7CF),
    surfaceContainerLowest = Color(0xFF090E13),
    surfaceContainerLow = Color(0xFF161C22),
    surfaceContainer = Color(0xFF1B2127),
    surfaceContainerHigh = Color(0xFF252B31),
    surfaceContainerHighest = Color(0xFF30363D)
)

private val OceanLightColors = lightColorScheme(
    primary = Color(0xFF124E78),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF506073),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4E4F6),
    onSecondaryContainer = Color(0xFF0C1D2A),
    tertiary = Color(0xFF256A5D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB4EFE1),
    onTertiaryContainer = Color(0xFF00201A),
    background = Color(0xFFF6FAFF),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFF6FAFF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4FA),
    surfaceContainer = Color(0xFFEAEFF6),
    surfaceContainerHigh = Color(0xFFE4E9F0),
    surfaceContainerHighest = Color(0xFFDEE3EA)
)

private val SunsetDarkColors = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5F1602),
    primaryContainer = Color(0xFF8C2F16),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFE7BDB0),
    onSecondary = Color(0xFF442A21),
    secondaryContainer = Color(0xFF5D4036),
    onSecondaryContainer = Color(0xFFFFDBD0),
    tertiary = Color(0xFFE8C46D),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE093),
    background = Color(0xFF18110F),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF18110F),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BB),
    surfaceContainerLowest = Color(0xFF120C0A),
    surfaceContainerLow = Color(0xFF211A17),
    surfaceContainer = Color(0xFF251E1B),
    surfaceContainerHigh = Color(0xFF302825),
    surfaceContainerHighest = Color(0xFF3B3330)
)

private val SunsetLightColors = lightColorScheme(
    primary = Color(0xFFBA4B2F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = Color(0xFF77574D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF2C150E),
    tertiary = Color(0xFF755A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE088),
    onTertiaryContainer = Color(0xFF241A00),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DDD6),
    onSurfaceVariant = Color(0xFF53433E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1ED),
    surfaceContainer = Color(0xFFFCEAE4),
    surfaceContainerHigh = Color(0xFFF6E4DE),
    surfaceContainerHighest = Color(0xFFF0DED8)
)

private val ForestDarkColors = darkColorScheme(
    primary = Color(0xFF8CD9B9),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF0D513C),
    onPrimaryContainer = Color(0xFFA8F6D5),
    secondary = Color(0xFFB3CCC0),
    onSecondary = Color(0xFF1F352C),
    secondaryContainer = Color(0xFF354B42),
    onSecondaryContainer = Color(0xFFCFE9DC),
    tertiary = Color(0xFFA7CCE0),
    onTertiary = Color(0xFF0B3445),
    tertiaryContainer = Color(0xFF264B5C),
    onTertiaryContainer = Color(0xFFC3E8FD),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4E0),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDEE4E0),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C2),
    surfaceContainerLowest = Color(0xFF0A100D),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633)
)

private val ForestLightColors = lightColorScheme(
    primary = Color(0xFF1E6B52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F6D5),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4C6358),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9DC),
    onSecondaryContainer = Color(0xFF092017),
    tertiary = Color(0xFF3E6375),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC3E8FD),
    onTertiaryContainer = Color(0xFF001E2B),
    background = Color(0xFFF5FBF7),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FBF7),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF404944),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F1),
    surfaceContainer = Color(0xFFE9EFEB),
    surfaceContainerHigh = Color(0xFFE3EAE5),
    surfaceContainerHighest = Color(0xFFDDE4DF)
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(42.dp)
)

private fun expressiveTypography(): Typography {
    val base = Typography()
    fun TextStyle.emphasized(weight: FontWeight = FontWeight.SemiBold) = copy(fontWeight = weight)
    return base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = base.headlineSmall.emphasized(),
        titleLarge = base.titleLarge.emphasized(FontWeight.Bold),
        titleMedium = base.titleMedium.emphasized(FontWeight.SemiBold),
        titleSmall = base.titleSmall.emphasized(FontWeight.SemiBold),
        labelLarge = base.labelLarge.emphasized(FontWeight.SemiBold),
        labelMedium = base.labelMedium.emphasized(FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp)
    )
}

internal fun ThemePalette.colorScheme(darkTheme: Boolean): ColorScheme = when (this) {
    ThemePalette.STUDIO -> if (darkTheme) StudioDarkColors else StudioLightColors
    ThemePalette.OCEAN -> if (darkTheme) OceanDarkColors else OceanLightColors
    ThemePalette.SUNSET -> if (darkTheme) SunsetDarkColors else SunsetLightColors
    ThemePalette.FOREST -> if (darkTheme) ForestDarkColors else ForestLightColors
}

internal fun ThemePalette.previewColors(darkTheme: Boolean): Pair<Color, Color> {
    val scheme = colorScheme(darkTheme)
    return scheme.primary to scheme.tertiary
}

internal fun ThemePalette.displayName(): String = when (this) {
    ThemePalette.STUDIO -> "Stüdyo"
    ThemePalette.OCEAN -> "Okyanus"
    ThemePalette.SUNSET -> "Gün Batımı"
    ThemePalette.FOREST -> "Orman"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PodcastTheme(
    darkTheme: Boolean = true,
    palette: ThemePalette = ThemePalette.STUDIO,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = palette.colorScheme(darkTheme),
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = expressiveTypography(),
        content = content
    )
}
