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
    val badgeHorizontal: Dp = 10.dp
    val badgeVertical: Dp = 4.dp
    val border: Dp = 1.dp
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
    primary = StickBlueContainer,
    onPrimary = StickInk,
    primaryContainer = StickBlue,
    onPrimaryContainer = StickWhite,
    secondary = StickGreenContainer,
    onSecondary = StickInk,
    secondaryContainer = StickGreen,
    onSecondaryContainer = StickWhite,
    error = StickRed,
    background = StickInk,
    onBackground = StickSurface,
    surface = ColorTokens.darkSurface,
    onSurface = StickSurface,
    surfaceContainerLow = ColorTokens.darkSurfaceSoft,
    surfaceContainerHighest = ColorTokens.darkSurfaceStrong,
    onSurfaceVariant = ColorTokens.darkOnSurfaceVariant,
    outline = ColorTokens.darkOutline,
    outlineVariant = ColorTokens.darkOutlineVariant,
)

private object ColorTokens {
    val darkSurface = StickInk
    val darkSurfaceSoft = StickSlate.copy(alpha = 0.22f)
    val darkSurfaceStrong = StickSlate.copy(alpha = 0.36f)
    val darkOnSurfaceVariant = StickPanel
    val darkOutline = StickPanelAlt
    val darkOutlineVariant = StickSlate
}

private val GpStickShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
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
