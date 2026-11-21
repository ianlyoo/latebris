package io.github.ianlyoo.latebris.ui.theme

import androidx.compose.ui.graphics.Color

// Wayfinding palette. Yellow marks what is being simulated right now, the way a
// direction sign marks where to go; the ink panel names where the device "is".

/** Sign yellow: live signals and the preset name on the location sign. */
val SignYellow = Color(0xFFFFCC00)
val SignYellowPressed = Color(0xFFE6B800)
val OnSignYellow = Color(0xFF16181C)

/** Sign ink: the location sign and primary controls, in both themes. */
val SignInk = Color(0xFF16181C)
val SignInkRaised = Color(0xFF23262C)
val OnSignInk = Color(0xFFF4F5F7)
val OnSignInkMuted = Color(0xFFB8BDC6)

// Light scheme: cool enamel greys, never cream.
val LightGround = Color(0xFFEEF0F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFF6F7F9)
val LightSurfaceHigh = Color(0xFFE3E6EB)
val LightOutline = Color(0xFF6E7480)
val LightOutlineVariant = Color(0xFFCDD1D8)
val LightText = Color(0xFF16181C)
val LightTextMuted = Color(0xFF4F5560)

// Dark scheme: graphite, with the same sign colours.
val DarkGround = Color(0xFF0F1114)
val DarkSurface = Color(0xFF181A1F)
val DarkSurfaceLow = Color(0xFF14161A)
val DarkSurfaceHigh = Color(0xFF24272D)
val DarkOutline = Color(0xFF8C929C)
val DarkOutlineVariant = Color(0xFF353941)
val DarkText = Color(0xFFF1F2F4)
val DarkTextMuted = Color(0xFFAAB0BA)

/** Exit-sign green: a requirement that is met. */
val ReadyGreenLight = Color(0xFF14773D)
val ReadyGreenDark = Color(0xFF5CC98A)

val ErrorLight = Color(0xFFB3261E)
val ErrorDark = Color(0xFFFFB4AB)
