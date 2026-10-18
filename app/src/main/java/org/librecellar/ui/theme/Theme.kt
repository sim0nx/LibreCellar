package org.librecellar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WineBurgundyLight,
    onPrimary = Color.White,
    primaryContainer = WineBurgundyDark,
    onPrimaryContainer = WineRose,
    secondary = CellarGold,
    onSecondary = DarkBackground,
    secondaryContainer = CellarOakDark,
    onSecondaryContainer = CellarGoldLight,
    tertiary = WineRose,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    outlineVariant = Color(0xFF4A343D)
)

private val LightColorScheme = lightColorScheme(
    primary = WineBurgundy,
    onPrimary = Color.White,
    primaryContainer = WineRoseLight,
    onPrimaryContainer = WineBurgundyDark,
    secondary = WineOakWoodTint(),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3ECE6),
    onSecondaryContainer = WineBurgundyDark,
    tertiary = CellarGold,
    onTertiary = DarkBackground,
    background = Color(0xFFFAF7F5),
    onBackground = Color(0xFF1E1719),
    surface = Color.White,
    onSurface = Color(0xFF1E1719),
    surfaceVariant = Color(0xFFF3ECE9),
    onSurfaceVariant = Color(0xFF6B585E),
    outline = Color(0xFFDAC7CD),
    outlineVariant = Color(0xFFEDE0E3)
)

private fun WineOakWoodTint() = Color(0xFF7A3E26)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to deep cellar aesthetic for luxury wine feel
    dynamicColor: Boolean = false, // Keep bespoke wine cellar palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
