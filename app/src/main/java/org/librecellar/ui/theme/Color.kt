package org.librecellar.ui.theme

import androidx.compose.ui.graphics.Color

// Wine Cellar Luxury Palette
val WineBurgundyDark = Color(0xFF2C0E1B)
val WineBurgundy = Color(0xFF6B1D2F)
val WineBurgundyMedium = Color(0xFF88253B)
val WineBurgundyLight = Color(0xFFA83246)
val WineRose = Color(0xFFE27B88)
val WineRoseLight = Color(0xFFF9EAEB)

// Oak & Cellar Accent Tones
val CellarGold = Color(0xFFD4AF37)
val CellarGoldLight = Color(0xFFF2D984)
val CellarOakWood = Color(0xFF5D4037)
val CellarOakDark = Color(0xFF3E2723)
val CellarStone = Color(0xFF261D20)
val CellarStoneSurface = Color(0xFF1E1619)
val CellarStoneSurfaceElevated = Color(0xFF2B2024)

// Neutral & Background
val DarkBackground = Color(0xFF140D10)
val DarkSurface = Color(0xFF1C1317)
val DarkSurfaceElevated = Color(0xFF261A20)
val DarkBorder = Color(0xFF3D2A32)

val TextPrimaryDark = Color(0xFFF5EBE6)
val TextSecondaryDark = Color(0xFFB8A2A9)
val TextTertiaryDark = Color(0xFF7E6B72)

val StatusOccupied = Color(0xFFC75D4D)
val StatusEmpty = Color(0xFF3F4E38)
val StatusEmptyBorder = Color(0xFF68875B)
val StatusWarning = Color(0xFFD99B26)

// Wine type colours, the same as the web app's (cellar slots, statistics)
private val WineTypeColors = mapOf(
    "RED" to Color(0xFF88253B),
    "WHITE" to Color(0xFFC9B458),
    "ROSE" to Color(0xFFE27B88),
    "SPARKLING" to Color(0xFFB8962E),
    "DESSERT" to Color(0xFFC68B25),
    "FORTIFIED" to Color(0xFF5D4037)
)

fun wineTypeColor(wineType: String?): Color = WineTypeColors[wineType?.uppercase()] ?: WineRose
