package com.example.gpstick.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GpStickSpacing {
    val micro: Dp = 4.dp
    val compact: Dp = 8.dp
    val stack: Dp = 12.dp
    val card: Dp = 16.dp
    val section: Dp = 20.dp
    val screen: Dp = 24.dp
    val frame: Dp = 28.dp
    val hero: Dp = 32.dp
    val drawerWidth: Dp = 320.dp
    val badgeHorizontal: Dp = 10.dp
    val badgeVertical: Dp = 4.dp
    val border: Dp = 1.dp
}

object GpStickAlpha {
    const val chromeGlow: Float = 0.16f
    const val chromeGlowSoft: Float = 0.08f
}

private val LightColors = lightColorScheme(
    primary = StickBlue,
    onPrimary = StickWhite,
    primaryContainer = StickBlueContainer,
    onPrimaryContainer = StickInk,
    secondary = StickGreen,
    onSecondary = StickWhite,
    secondaryContainer = StickGreenContainer,
    onSecondaryContainer = StickInk,
    error = StickRed,
    background = StickSurface,
    onBackground = StickInk,
    surface = StickWhite,
    onSurface = StickInk,
    surfaceContainerLow = StickSurface,
    surfaceContainerHighest = StickPanel,
    onSurfaceVariant = StickSlate,
    outline = StickOutline,
    outlineVariant = StickPanelAlt,
)

private val DarkColors = darkColorScheme(
    primary = StickCyan,
    onPrimary = StickNight,
    primaryContainer = StickCyanContainer,
    onPrimaryContainer = StickCloud,
    secondary = StickMint,
    onSecondary = StickNight,
    secondaryContainer = StickMintContainer,
    onSecondaryContainer = StickCloud,
    tertiary = StickAmber,
    onTertiary = StickNight,
    error = StickRed,
    errorContainer = StickRedContainer,
    onError = StickNight,
    onErrorContainer = StickCloud,
    background = StickNight,
    onBackground = StickCloud,
    surface = StickNightSurface,
    onSurface = StickCloud,
    surfaceContainerLow = StickNightSurfaceSoft,
    surfaceContainerHighest = StickNightSurfaceStrong,
    onSurfaceVariant = StickMist,
    outline = StickOutline,
    outlineVariant = StickOutlineSoft,
)

private val GpStickShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(999.dp),
)

@Composable
fun GpStickTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GpStickTypography,
        shapes = GpStickShapes,
        content = content,
    )
}
