package io.github.ianlyoo.latebris.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object LatebrisSpacing {
    val hairline: Dp = 1.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    /** Side margin of every screen. */
    val screen: Dp = 16.dp
    /** Space above a section heading; below it is [sm]. */
    val sectionGap: Dp = 28.dp
    /** Room at the bottom of scrolling content so the FAB never covers the last row. */
    val fabClearance: Dp = 96.dp
}

/** Colours outside Material's roles: the sign pair and the "requirement met" green. */
@Immutable
data class SignColors(
    val yellow: Color,
    val onYellow: Color,
    val ink: Color,
    val inkRaised: Color,
    val onInk: Color,
    val onInkMuted: Color,
    val ready: Color,
)

private val Signs = SignColors(
    yellow = SignYellow,
    onYellow = OnSignYellow,
    ink = SignInk,
    inkRaised = SignInkRaised,
    onInk = OnSignInk,
    onInkMuted = OnSignInkMuted,
    ready = ReadyGreenLight,
)

val LocalSignColors = staticCompositionLocalOf { Signs }

private val LightColors = lightColorScheme(
    primary = SignInk,
    onPrimary = OnSignInk,
    primaryContainer = SignYellow,
    onPrimaryContainer = OnSignYellow,
    secondary = LightTextMuted,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceHigh,
    onSecondaryContainer = LightText,
    tertiary = ReadyGreenLight,
    onTertiary = LightSurface,
    background = LightGround,
    onBackground = LightText,
    surface = LightGround,
    onSurface = LightText,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextMuted,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight,
    onError = LightSurface,
)

// Primary is a neutral in both themes (ink by day, enamel white by night), so yellow
// keeps one meaning: what is live, and where you are.
private val DarkColors = darkColorScheme(
    primary = OnSignInk,
    onPrimary = SignInk,
    primaryContainer = SignYellow,
    onPrimaryContainer = OnSignYellow,
    secondary = DarkTextMuted,
    onSecondary = DarkGround,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkText,
    tertiary = ReadyGreenDark,
    onTertiary = DarkGround,
    background = DarkGround,
    onBackground = DarkText,
    surface = DarkGround,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextMuted,
    surfaceContainerLowest = DarkSurfaceLow,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark,
    onError = DarkGround,
)

// Enamel sign plates: small radii, never pills.
private val LatebrisShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun LatebrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val signs = if (darkTheme) Signs.copy(ready = ReadyGreenDark, inkRaised = DarkSurfaceHigh) else Signs
    androidx.compose.runtime.CompositionLocalProvider(LocalSignColors provides signs) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = LatebrisTypography,
            shapes = LatebrisShapes,
            content = content,
        )
    }
}
